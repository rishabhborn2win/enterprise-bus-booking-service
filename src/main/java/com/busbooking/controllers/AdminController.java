package com.busbooking.controllers;

import com.busbooking.domains.dto.admin.BusRequestDto;
import com.busbooking.domains.dto.admin.BusResponseDto;
import com.busbooking.domains.dto.admin.RouteRequestDto;
import com.busbooking.domains.dto.admin.RouteResponseDto;
import com.busbooking.domains.dto.admin.ScheduleRequestDto;
import com.busbooking.domains.dto.admin.ScheduleResponseDto;
import com.busbooking.domains.dto.admin.StopRequestDto;
import com.busbooking.domains.dto.admin.StopResponseDto;
import com.busbooking.entities.Bus;
import com.busbooking.entities.Route;
import com.busbooking.entities.Schedule;
import com.busbooking.entities.ScheduleStop;
import com.busbooking.entities.Stop;
import com.busbooking.services.AdminService;
import com.busbooking.services.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(
        name = "Admin Management",
        description = "Endpoints for managing Buses, Routes, and Schedules (CRUD + Sync)")
public class AdminController {

    private final AdminService adminService;
    private final SyncService syncService;

    // -----------------------------------------------------------------
    // | 1. BUS MANAGEMENT ENDPOINTS                                   |
    // -----------------------------------------------------------------

    @Operation(
            summary = "Register a new Bus",
            description = "Adds a new bus (fleet vehicle) to the system.")
    @PostMapping("/bus")
    public ResponseEntity<BusResponseDto> addBus(@RequestBody BusRequestDto request) {
        // Assume BusRequestDto has a toEntity() method.
        Bus newBus = request.toEntity();
        Bus savedBus = adminService.saveBus(newBus);
        return ResponseEntity.ok(BusResponseDto.fromEntity(savedBus));
    }

    @Operation(
            summary = "Update Bus details",
            description =
                    "Modifies existing details (e.g., operator, total seats) of a registered bus.")
    @PutMapping("/bus/{busId}")
    public ResponseEntity<BusResponseDto> updateBus(
            @PathVariable Long busId, @RequestBody BusRequestDto request) {
        // Assume toEntity(id) sets the ID correctly for JPA update.
        Bus busDetails = request.toEntity(busId);
        Bus updatedBus = adminService.updateBus(busId, busDetails);
        return ResponseEntity.ok(BusResponseDto.fromEntity(updatedBus));
    }

    // -----------------------------------------------------------------
    // | 2. ROUTE MANAGEMENT ENDPOINTS                                 |
    // -----------------------------------------------------------------

    @Operation(
            summary = "Create/Update Route",
            description =
                    "Registers a new logical route (Source-Destination pair) or updates an existing one.")
    @PostMapping("/route")
    public ResponseEntity<RouteResponseDto> addRoute(@RequestBody RouteRequestDto request) {
        // This relies on AdminService checking if Stops exist and mapping DTO to Entity.
        Route savedRoute = adminService.createOrUpdateRoute(request);
        return ResponseEntity.ok(RouteResponseDto.fromEntity(savedRoute));
    }

    // -----------------------------------------------------------------
    // | 3. SCHEDULE MANAGEMENT ENDPOINTS (Existing Logic)             |
    // -----------------------------------------------------------------

    @Operation(
            summary = "Create or Update Schedule",
            description =
                    "Registers a specific bus run for an existing route. This includes defining all intermediate stops (Schedule_Stop entities), triggering a bus conflict check, and syncing to Elasticsearch.")
    @PostMapping("/schedule")
    public ResponseEntity<ScheduleResponseDto> addSchedule(
            @RequestBody ScheduleRequestDto request) {
        // Mock DTO to Entity conversion for example
        Schedule newSchedule = request.toScheduleEntity();
        List<ScheduleStop> stops = request.toScheduleStopEntities(newSchedule);
        newSchedule.setStops(stops);

        Schedule savedSchedule = adminService.createOrUpdateSchedule(newSchedule, stops);

        return ResponseEntity.ok(ScheduleResponseDto.fromEntity(savedSchedule));
    }

    // -----------------------------------------------------------------
    // | 4. SYNCHRONIZATION ENDPOINTS                                  |
    // -----------------------------------------------------------------

    @Operation(
            summary = "Manually Trigger Full Sync",
            description =
                    "Pulls ALL active schedules from MySQL and rebuilds the Elasticsearch index. Use for initialization or recovery.")
    @PostMapping("/sync/full")
    public ResponseEntity<String> triggerFullSync() {
        syncService.performFullSync();
        return ResponseEntity.ok("Full synchronization initiated successfully.");
    }

    // -----------------------------------------------------------------
    // | NEW: STOP MANAGEMENT ENDPOINTS                                |
    // -----------------------------------------------------------------

    @Operation(
            summary = "Add a new Stop",
            description = "Registers a new stop/city to be used in routes.")
    @PostMapping("/stop")
    public ResponseEntity<StopResponseDto> addStop(@RequestBody StopRequestDto request) {
        Stop savedStop = adminService.saveStop(request);
        return ResponseEntity.ok(StopResponseDto.fromEntity(savedStop));
    }

    @Operation(
            summary = "Update Stop details",
            description = "Modifies the name or city of an existing stop.")
    @PutMapping("/stop/{stopId}")
    public ResponseEntity<StopResponseDto> updateStop(
            @PathVariable Long stopId, @RequestBody StopRequestDto request) {
        Stop updatedStop = adminService.updateStop(stopId, request);
        return ResponseEntity.ok(StopResponseDto.fromEntity(updatedStop));
    }
}
