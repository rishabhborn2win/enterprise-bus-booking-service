package com.busbooking.controllers;

import com.busbooking.domains.dto.admin.ScheduleRequestDto;
import com.busbooking.domains.dto.admin.ScheduleResponseDto;
import com.busbooking.entities.Schedule;
import com.busbooking.entities.ScheduleStop;
import com.busbooking.services.AdminService;
import com.busbooking.services.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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

    @Operation(
            summary = "Create or Update Schedule",
            description =
                    "Adds a new schedule and its sequential stops in MySQL, triggering an immediate sync to Elasticsearch.")
    @PostMapping("/schedule")
    public ResponseEntity<ScheduleResponseDto> addSchedule(
            @RequestBody ScheduleRequestDto request) {
        // Mock DTO to Entity conversion for example
        Schedule newSchedule = request.toScheduleEntity();
        List<ScheduleStop> stops = request.toScheduleStopEntities(newSchedule);

        Schedule savedSchedule = adminService.createOrUpdateSchedule(newSchedule, stops);

        return ResponseEntity.ok(ScheduleResponseDto.fromEntity(savedSchedule));
    }

    @Operation(
            summary = "Manually Trigger Full Sync",
            description =
                    "Pulls ALL active schedules from MySQL and rebuilds the Elasticsearch index. Use for initialization or recovery.")
    @PostMapping("/sync/full")
    public ResponseEntity<String> triggerFullSync() {
        syncService.performFullSync();
        return ResponseEntity.ok("Full synchronization initiated successfully.");
    }
}
