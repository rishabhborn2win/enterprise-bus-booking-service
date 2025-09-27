package com.busbooking.services;

import com.busbooking.entities.Bus;
import com.busbooking.entities.Route;
import com.busbooking.entities.Schedule;
import com.busbooking.entities.ScheduleStop;
import com.busbooking.repositories.BusRepository;
import com.busbooking.repositories.RouteRepository;
import com.busbooking.repositories.ScheduleRepository;
import com.busbooking.repositories.ScheduleStopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

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
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Bus not found with ID: " + id));

        bus.setOperator(busDetails.getOperator());
        bus.setTotalSeats(busDetails.getTotalSeats());
        return busRepository.save(bus);
    }

    // --- 2. ROUTE Management (CRUD) ---

    public Route saveRoute(Route route) {
        // Validation: Ensure source and dest stops exist before saving (omitted for brevity)
        return routeRepository.save(route);
    }

    public Route updateRoute(Integer id, Route routeDetails) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Route not found with ID: " + id));

        // Update distance and possibly source/dest stops (complex update logic omitted)
        route.setDistanceKm(routeDetails.getDistanceKm());

        return routeRepository.save(route);
    }

    // --- 3. SCHEDULE Management (CRUD + Sync) ---

    /**
     * Creates a new Schedule or modifies an existing one, and ensures stop consistency.
     * Triggers an immediate synchronization to Elasticsearch upon successful commit.
     * @param schedule The Schedule entity (new or existing)
     * @param stops The complete list of ScheduleStop entities for this schedule
     * @return The saved Schedule entity
     */
    @Transactional // Ensures atomicity for Schedule and ScheduleStop tables
    public Schedule createOrUpdateSchedule(Schedule schedule, List<ScheduleStop> stops) {

        // 1. Save/Update Schedule
        Schedule savedSchedule = scheduleRepository.save(schedule);

        // 2. Handle Schedule Stops

        // **If updating, delete old stops first to manage the unique key (schedule_id, stop_order)**
        if (schedule.getId() != null) {
            scheduleStopRepository.deleteByScheduleId(schedule.getId());
        }

        // **Associate new stops with the saved schedule and save them**
        stops.forEach(stop -> {
            stop.setSchedule(savedSchedule); // Link back to the parent schedule
            // Note: Stop entity must be managed or correctly referenced before saving
        });
        scheduleStopRepository.saveAll(stops);

        // Important: Re-fetch the schedule with its newly created stops collection
        // This is often needed if the JPA session is complex.
        Schedule finalSchedule = scheduleRepository.findById(savedSchedule.getId())
                .orElseThrow(() -> new NoSuchElementException("Failed to retrieve schedule after save."));

        // 3. Trigger Immediate Sync to Elasticsearch
        // This ensures the search index reflects the new schedule data instantly.
        syncService.syncScheduleToElasticsearch(finalSchedule.getId());

        return finalSchedule;
    }

    public void deleteSchedule(Long id) {
        // Delete ScheduleStops first due to FK constraints (or rely on CascadeType.ALL)
        scheduleStopRepository.deleteByScheduleId(id);
        scheduleRepository.deleteById(id);

        // Delete from Elasticsearch as well
        syncService.deleteScheduleFromElasticsearch(id); // Requires new method in SyncService
    }
}