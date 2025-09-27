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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations; // NEW IMPORT
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// ** In: com.bussystem.controller.SearchController **
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(
        name = "Bus Search & Availability",
        description =
                "High-speed endpoints for finding schedules and checking segment-based seat availability.")
public class SearchController {

    private final BusScheduleElasticRepository elasticRepository;
    private final ElasticsearchOperations elasticsearchOperations; // NEW INJECTION
    private final SeatAvailabilityService availabilityService;

    @Operation(
            summary = "Search Available Schedules",
            description =
                    "Queries Elasticsearch for schedules containing the requested segment (intermediate stops supported).")
    @GetMapping("/schedules")
    public ResponseEntity<Page<BusScheduleSearchResponse>> searchSchedules(
            @RequestParam Long sourceStopId,
            @RequestParam Long destinationStopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // --- 1. Build the Segment Search Criteria (The Fix) ---

        // 🔑 We need schedules that contain both stops AND run on the correct date.
        Criteria criteria =
                new Criteria("departureTime")
                        .between(startOfDay, endOfDay) // Date range criteria
                        .and(
                                new Criteria("stops.stopId")
                                        .is(sourceStopId)) // Source stop must exist
                        .and(
                                new Criteria("stops.stopId")
                                        .is(destinationStopId)); // Destination stop must exist

        // Note: Spring Data automatically handles the nested query structure
        // when using the Criteria API on nested fields like 'stops.stopId'.

        Query query = new CriteriaQuery(criteria).setPageable(pageable);

        // --- 2. Execute Elasticsearch Search using the Template ---

        SearchHits<BusScheduleDocument> searchHits =
                elasticsearchOperations.search(query, BusScheduleDocument.class);

        // 3. Convert SearchHits to Page (Standard Spring Data Utility)
        Page<BusScheduleDocument> documentsPage =
                PageableExecutionUtils.getPage(
                        searchHits.getSearchHits().stream().map(SearchHit::getContent).toList(),
                        pageable,
                        () -> elasticsearchOperations.count(query, BusScheduleDocument.class));

        // --- 4. Segment-based Availability Check (Mapping remains the same) ---

        Page<BusScheduleSearchResponse> responsePage =
                documentsPage.map(
                        doc -> {
                            // Check segment order and seat availability using MySQL data
                            SeatAvailabilityResponse availability =
                                    availabilityService.getSegmentAvailability(
                                            doc.getId(), sourceStopId, destinationStopId);

                            return new BusScheduleSearchResponse(
                                    doc,
                                    availability.getTotalSeats(),
                                    availability.getBookedSeats().size(),
                                    availability.getAvailableSeats().size());
                        });

        return ResponseEntity.ok(responsePage);
    }

    @Operation(
            summary = "Get Segment Seat Map",
            description =
                    "Returns a detailed seat map (available/booked) specific to the requested segment (Source -> Destination) using the complex overlap logic.")
    @GetMapping("/schedule/{scheduleId}/seat-map")
    public ResponseEntity<SeatMapResponse> getSeatMap(
            @PathVariable Long scheduleId,
            @Parameter(description = "ID of the boarding stop for this segment.", required = true)
                    @RequestParam
                    Long sourceStopId,
            @Parameter(
                            description = "ID of the de-boarding stop for this segment.",
                            required = true)
                    @RequestParam
                    Long destinationStopId) {
        // Fetch segment-based availability
        SeatAvailabilityResponse availability =
                availabilityService.getSegmentAvailability(
                        scheduleId, sourceStopId, destinationStopId);

        return ResponseEntity.ok(
                new SeatMapResponse(
                        availability.getTotalSeats(),
                        availability.getAvailableSeats(),
                        availability.getBookedSeats(),
                        availability.getSeatMap()));
    }
}
