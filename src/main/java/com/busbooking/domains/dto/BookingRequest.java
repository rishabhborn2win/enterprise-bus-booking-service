package com.busbooking.domains.dto;

import com.busbooking.enums.BookingStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for the initial seat reservation request.
 * Includes Jakarta Bean Validation for input quality.
 */
@Data
public class BookingRequest {

    @NotNull(message = "User ID is required for a booking.")
    private Long userId;

    @NotNull(message = "Schedule ID is required to identify the bus trip.")
    private Long scheduleId;

    @NotNull(message = "Start stop ID is required for multi-hop validation.")
    private Long startStopId;

    @NotNull(message = "End stop ID is required for multi-hop validation.")
    private Long endStopId;

    @NotEmpty(message = "At least one seat must be selected or auto-assigned.")
    @Size(min = 1, max = 5, message = "Maximum 5 seats allowed per booking request.")
    private List<String> seatNumbers; // Can be manually selected or null for auto-assignment

    private List<Integer> addonIds; // Optional addons (Extra luggage, meal, etc.)
}

