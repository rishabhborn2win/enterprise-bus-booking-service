package com.busbooking.services;

import com.busbooking.entities.Bus;
import com.busbooking.entities.Route;
import com.busbooking.entities.Schedule;
import com.busbooking.entities.ScheduleStop;
import com.busbooking.exceptions.ResourceNotFoundException;
import com.busbooking.repositories.BusRepository;
import com.busbooking.repositories.RouteRepository;
import com.busbooking.repositories.ScheduleRepository;
import com.busbooking.repositories.ScheduleStopRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final ScheduleRepository scheduleRepository;
    private final ScheduleStopRepository scheduleStopRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final SyncService syncService;

    // --- 1. BUS Management (CRUD) ---

    public Bus saveBus(Bus bus) {
        return busRepository.save(bus);
    }

    public Bus updateBus(Long id, Bus busDetails) {
        Bus bus =
                busRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new NoSuchElementException("Bus not found with ID: " + id));

        bus.setOperator(busDetails.getOperator());
        bus.setTotalSeats(busDetails.getTotalSeats());
        return busRepository.save(bus);
    }

    // --- 2. ROUTE Management (CRUD) ---

    public Route saveRoute(Route route) {
        // Validation: Ensure source and dest stops exist before saving (omitted for brevity)
        return routeRepository.save(route);
    }

    public Route updateRoute(Long id, Route routeDetails) {
        Route route =
                routeRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new NoSuchElementException("Route not found with ID: " + id));

        // Update distance and possibly source/dest stops (complex update logic omitted)
        route.setDistanceKm(routeDetails.getDistanceKm());

        return routeRepository.save(route);
    }

    // --- 3. SCHEDULE Management (CRUD + Sync) ---

    /**
     * Creates a new Schedule or modifies an existing one, and ensures stop consistency. Triggers an
     * immediate synchronization to Elasticsearch upon successful commit.
     *
     * @param schedule The Schedule entity (new or existing)
     * @param stops The complete list of ScheduleStop entities for this schedule
     * @return The saved Schedule entity
     */
    @Transactional // Ensures atomicity for Schedule and ScheduleStop tables
    public Schedule createOrUpdateSchedule(Schedule schedule, List<ScheduleStop> stops) {
        // --- VALIDATION ---
        if (schedule.getBus() == null || schedule.getBus().getId() == null) {
            throw new IllegalArgumentException("Schedule must be associated with a bus.");
        }
        if (schedule.getRoute() == null || schedule.getRoute().getId() == null) {
            throw new IllegalArgumentException("Schedule must be associated with a route.");
        }
        if (schedule.getBasePrice() == null || schedule.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Base price must be positive.");
        }
        if (stops == null || stops.isEmpty()) {
            throw new IllegalArgumentException("Schedule must have at least one stop.");
        }

        // Validate stop order uniqueness and sequence
        Set<Integer> stopOrders = new HashSet<>();
        for (ScheduleStop stop : stops) {
            if (!stopOrders.add(stop.getStopOrder())) {
                throw new IllegalArgumentException("Duplicate stop order found: " + stop.getStopOrder());
            }
        }

        // --- OVERLAP VALIDATION ---
        Route route = routeRepository.findById(schedule.getRoute().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", schedule.getRoute().getId()));

        LocalDateTime startTime = schedule.getDepartureTime();
        LocalDateTime endTime = schedule.getStops().stream()
                .filter(s -> s.getStopOrder().equals(stops.size())) // Last stop
                .findFirst()
                .map(ScheduleStop::getArrivalTime)
                .orElseThrow();
        Long scheduleIdToExclude = schedule.getId() != null ? schedule.getId() : -1L; // Exclude self if updating

        //ToDo: @rishabh.mishra Add a validation to make sure no overlapping schedule for same bus
        List<Schedule> overlappingSchedules = null;

        if (!overlappingSchedules.isEmpty()) {
            throw new IllegalStateException("Bus is already scheduled for an overlapping time period.");
        }

        // --- PERSISTENCE ---
        Schedule savedSchedule = scheduleRepository.save(schedule);

        if (savedSchedule.getId() != null && !savedSchedule.getId().equals(schedule.getId())) {
            scheduleStopRepository.deleteByScheduleId(savedSchedule.getId());
        }

        stops.forEach(stop -> stop.setSchedule(savedSchedule));
        scheduleStopRepository.saveAll(stops);

        syncService.syncScheduleToElasticsearch(savedSchedule.getId());

        return savedSchedule;
    }

    public void deleteSchedule(Long id) {
        // Delete ScheduleStops first due to FK constraints (or rely on CascadeType.ALL)
        scheduleStopRepository.deleteByScheduleId(id);
        scheduleRepository.deleteById(id);

        // Delete from Elasticsearch as well
        syncService.deleteScheduleFromElasticsearch(id); // Requires new method in SyncService
    }
}
