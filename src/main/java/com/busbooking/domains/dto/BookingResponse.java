package com.busbooking.domains.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List; /**
 * DTO for the booking response, simplifying the full Entity.
 */
@Data
public class BookingResponse {
    private String bookingId;
    private Long scheduleId;
    private String userTicketRef;
    private com.busbooking.enums.BookingStatus status;
    private BigDecimal finalPrice;
    private LocalDateTime bookingTime;
    private LocalDateTime expirationTime;
    private List<String> reservedSeats;
    private List<String> addons;
}
