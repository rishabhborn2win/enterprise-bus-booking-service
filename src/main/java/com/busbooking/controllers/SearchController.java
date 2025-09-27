package com.busbooking.controllers;

import com.busbooking.domains.dto.BusScheduleDocument;
import com.busbooking.domains.dto.BusScheduleSearchResponse;
import com.busbooking.domains.dto.SeatAvailabilityResponse;
import com.busbooking.domains.dto.SeatMapResponse;
import com.busbooking.repositories.BusScheduleElasticRepository;
import com.busbooking.services.SeatAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// ** In: com.bussystem.controller.SearchController **
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Bus Search & Availability", description = "High-speed endpoints for finding schedules and checking segment-based seat availability.")
public class SearchController {

    private final BusScheduleElasticRepository elasticRepository;
    private final SeatAvailabilityService availabilityService;

    @Operation(summary = "Search Available Schedules", description = "Queries Elasticsearch for matching schedules and calculates segment-based available seats in real-time from MySQL.")
    @GetMapping("/schedules")
    public ResponseEntity<Page<BusScheduleSearchResponse>> searchSchedules(
        @Parameter(description = "ID of the boarding stop.", required = true)
        @RequestParam Long sourceStopId,
        @Parameter(description = "ID of the de-boarding stop.", required = true)
        @RequestParam Long destinationStopId,
        @Parameter(description = "Travel Date (YYYY-MM-DD).", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // 1. Elasticsearch Search
        Page<BusScheduleDocument> documentsPage =
                elasticRepository.findBySourceStopIdAndDestinationStopIdAndDepartureTimeBetween(
            sourceStopId, destinationStopId, startOfDay, endOfDay, pageable
        );

        // 2. Segment-based Availability Check (Mapping)
        Page<BusScheduleSearchResponse> responsePage = documentsPage.map(doc -> {
            // Check availability for the requested segment
            SeatAvailabilityResponse availability = availabilityService.getSegmentAvailability(
                doc.getId(), sourceStopId, destinationStopId
            );

            return new BusScheduleSearchResponse(
                doc, 
                availability.getTotalSeats(),
                availability.getBookedSeats().size(),
                availability.getAvailableSeats().size()
            );
        });

        return ResponseEntity.ok(responsePage);
    }

    @Operation(summary = "Get Segment Seat Map", description = "Returns a detailed seat map (available/booked) specific to the requested segment (Source -> Destination) using the complex overlap logic.")
    @GetMapping("/schedule/{scheduleId}/seat-map")
    public ResponseEntity<SeatMapResponse> getSeatMap(
        @PathVariable Long scheduleId,
        @Parameter(description = "ID of the boarding stop for this segment.", required = true)
        @RequestParam Long sourceStopId,
        @Parameter(description = "ID of the de-boarding stop for this segment.", required = true)
        @RequestParam Long destinationStopId
    ) {
        // Fetch segment-based availability
        SeatAvailabilityResponse availability = availabilityService.getSegmentAvailability(
            scheduleId, 
            sourceStopId, 
            destinationStopId
        );

        return ResponseEntity.ok(new SeatMapResponse(
            availability.getTotalSeats(),
            availability.getAvailableSeats(),
            availability.getBookedSeats(),
            availability.getSeatMap()
        ));
    }
}