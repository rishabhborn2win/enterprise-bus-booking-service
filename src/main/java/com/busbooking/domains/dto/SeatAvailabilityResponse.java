package com.busbooking.domains.dto;

import com.busbooking.enums.SeatClass;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class SeatAvailabilityResponse {
    
    private Integer totalSeats;
    private List<String> bookedSeats;
    private List<String> availableSeats;
    private Map<String, SeatClass> seatMap; // Seat number to class map
}