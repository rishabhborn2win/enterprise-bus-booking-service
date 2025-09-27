package com.busbooking.domains.dto;

import com.busbooking.enums.SeatClass;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

// In: com.bussystem.dto.SeatMapResponse
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapResponse {
    
    @Schema(description = "Total physical seats on the bus.")
    private Integer totalSeats;

    @Schema(description = "List of seat numbers available for the requested segment.")
    private List<String> availableSeats;

    @Schema(description = "List of seat numbers unavailable (booked) for the requested segment.")
    private List<String> bookedSeats;
    
    @Schema(description = "Map of Seat Number -> Seat Class (e.g., '1A' -> 'Sleeper', '2B' -> 'Semi-Sleeper').")
    private Map<String, SeatClass> seatClassMap;
}