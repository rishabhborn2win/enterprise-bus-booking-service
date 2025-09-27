package com.busbooking.domains.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// In: com.bussystem.dto.BusScheduleSearchResponse
@Data
@NoArgsConstructor
public class BusScheduleSearchResponse {

    @Schema(description = "Details fetched from the high-speed Elasticsearch index.")
    private BusScheduleDocument scheduleDetails;

    @Schema(description = "Total physical seating capacity of the bus.")
    private Integer totalCapacity;

    @Schema(description = "Number of seats unavailable for the *requested segment* (A->B).")
    private Integer bookedSeatsForSegment;

    @Schema(description = "Number of seats available for the *requested segment* (A->B).")
    private Integer availableSeatsForSegment;
    
    // Constructor to map from ES Document and availability calculation
    public BusScheduleSearchResponse(BusScheduleDocument doc, Integer totalCapacity, Integer bookedCount, Integer availableCount) {
        this.scheduleDetails = doc;
        this.totalCapacity = totalCapacity;
        this.bookedSeatsForSegment = bookedCount;
        this.availableSeatsForSegment = availableCount;
    }
}