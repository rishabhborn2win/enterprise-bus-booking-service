package com.busbooking.services;

import com.busbooking.domains.dto.admin.RouteRequestDto;
import com.busbooking.domains.dto.admin.StopRequestDto;
import com.busbooking.entities.Bus;
import com.busbooking.entities.Route;
import com.busbooking.entities.Schedule;
import com.busbooking.entities.ScheduleStop;
import com.busbooking.entities.Seat;
import com.busbooking.entities.Stop;
import com.busbooking.exceptions.ResourceNotFoundException;
import com.busbooking.repositories.BusRepository;
import com.busbooking.repositories.RouteRepository;
import com.busbooking.repositories.ScheduleRepository;
import com.busbooking.repositories.ScheduleStopRepository;
import com.busbooking.repositories.SeatRepository;
import com.busbooking.repositories.StopRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
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
    private final StopRepository stopRepository;
    private final SyncService syncService;
    private final SeatRepository seatRepository;

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

    // --- Helper Method: Determines when the bus becomes available ---
    private LocalDateTime calculateScheduleTerminationTime(List<ScheduleStop> stops) {
        if (stops == null || stops.isEmpty()) {
            throw new IllegalArgumentException("Schedule must contain at least one stop.");
        }
        // Find the maximum arrival time based on the highest stop order
        return stops.stream()
                .max((ss1, ss2) -> Integer.compare(ss1.getStopOrder(), ss2.getStopOrder()))
                .map(ScheduleStop::getArrivalTime)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Could not determine schedule termination time."));
    }

    // --- Bus Conflict Check Execution ---
    private void checkBusSchedulingConflict(
            Long busId,
            Long scheduleIdToExclude,
            LocalDateTime startTime,
            LocalDateTime terminationTime) {

        List<Schedule> schedules = scheduleRepository.findByBusId(busId);
        List<Long> conflictingSchedules =
                schedules.stream()
                        .map(
                                s -> {
                                    List<ScheduleStop> stops =
                                            scheduleStopRepository.findByScheduleId(s.getId());
                                    LocalDateTime existingStart = s.getDepartureTime();
                                    LocalDateTime existingEnd =
                                            stops.stream()
                                                    .max(
                                                            (ss1, ss2) ->
                                                                    Integer.compare(
                                                                            ss1.getStopOrder(),
                                                                            ss2.getStopOrder()))
                                                    .map(ScheduleStop::getArrivalTime)
                                                    .orElse(existingStart); // Fallback to start
                                    // time if no stops

                                    // Check for overlap
                                    if (s.getId() != scheduleIdToExclude
                                            && existingStart.isBefore(terminationTime)
                                            && existingEnd.isAfter(startTime)) {
                                        return s.getId();
                                    }
                                    return null;
                                })
                        .filter(Objects::nonNull)
                        .toList();

        if (!conflictingSchedules.isEmpty()) {
            // Throwing a runtime exception ensures the transaction will rollback
            throw new IllegalStateException(
                    "Bus ID "
                            + busId
                            + " is already scheduled during this time slot. Conflicting Schedule IDs: "
                            + conflictingSchedules);
        }
    }

    // --- Core Schedule Creation/Update Logic ---

    @Transactional
    public Schedule createOrUpdateSchedule(
            Schedule schedule, List<ScheduleStop> stops, List<Seat> seats) {
        // 1. CALCULATE WINDOW & CHECK FOR CONFLICT
        LocalDateTime terminationTime = calculateScheduleTerminationTime(stops);
        LocalDateTime startTime = schedule.getDepartureTime();
        Long busId = schedule.getBus().getId();
        // Set the ID to -1L for a new schedule to prevent accidental exclusion of existing records
        Long scheduleIdToExclude = schedule.getId() != null ? schedule.getId() : -1L;

        checkBusSchedulingConflict(busId, scheduleIdToExclude, startTime, terminationTime);

        Route route =
                routeRepository
                        .findById(schedule.getRoute().getId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "route",
                                                "Route not found with ID: "
                                                        + schedule.getRoute().getId(),
                                                null));
        schedule.setRoute(route);

        // 2. PERSIST Schedule (Actual Persistence)
        Schedule savedSchedule = scheduleRepository.save(schedule);

        stops.forEach(stop -> stop.setSchedule(savedSchedule));
        scheduleStopRepository.saveAll(stops);

        // Retrieve the final entity (often required to ensure relationships are loaded before sync)
        Schedule finalSchedule =
                scheduleRepository
                        .findById(savedSchedule.getId())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Failed to retrieve schedule after save."));

        // Link new seat entities to the saved schedule and save them
        seats.forEach(
                seat -> {
                    seat.setSchedule(savedSchedule);
                    // Note: Assumes SeatRequestDto provided basic Seat objects
                });
        seatRepository.saveAll(seats);

        // 4. TRIGGER IMMEDIATE SYNC to Elasticsearch
        syncService.syncScheduleToElasticsearch(finalSchedule.getId());

        return finalSchedule;
    }

    // --- NEW: ROUTE Management (Transactional CRUD) ---

    @Transactional
    public Route createOrUpdateRoute(RouteRequestDto request) {
        // 1. Fetch Stop Entities (Ensure they exist and get the full JPA objects)
        Stop source =
                stopRepository
                        .findById(request.getSourceStopId())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Route creation failed: Source Stop not found with ID "
                                                        + request.getSourceStopId()));

        Stop destination =
                stopRepository
                        .findById(request.getDestStopId())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Route creation failed: Destination Stop not found with ID "
                                                        + request.getDestStopId()));

        // Validation: Prevent routing to self (optional, but good practice)
        if (source.getId().equals(destination.getId())) {
            throw new IllegalArgumentException("Source and Destination stops cannot be the same.");
        }

        // 2. Map DTO to Entity and attach the managed Stop objects
        // The RouteRequestDto's toEntity relies on the service passing the fetched Stops
        Route route = new Route();
        route.setId(request.getId()); // ID is null for POST, set for PUT
        route.setSourceStop(source);
        route.setDestinationStop(destination);
        route.setDistanceKm(request.getDistanceKm());

        // 3. Save and return the managed entity
        return routeRepository.save(route);
    }

    // -----------------------------------------------------------------
    // | NEW: STOP MANAGEMENT (CRUD)                                   |
    // -----------------------------------------------------------------

    @Transactional
    public Stop saveStop(StopRequestDto request) {
        Stop stop = request.toEntity();
        // ID should be null for new stop creation
        return stopRepository.save(stop);
    }

    @Transactional
    public Stop updateStop(Long id, StopRequestDto request) {
        Stop existingStop =
                stopRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new NoSuchElementException("Stop not found with ID: " + id));

        // Update fields
        existingStop.setName(request.getName());
        existingStop.setCity(request.getCity());

        return stopRepository.save(existingStop);
    }
}
