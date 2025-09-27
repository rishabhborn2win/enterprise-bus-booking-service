package com.busbooking.controllers;

import com.busbooking.domains.dto.BookingRequest;
import com.busbooking.domains.dto.BookingResponse;
import com.busbooking.services.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Booking Operations. Defines the REST API endpoints. Includes OpenAPI 3.0
 * annotations for interactive documentation.
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(
        name = "Booking Operations",
        description = "CRUD operations and concurrency management for seat reservations.")
public class BookingController {

    private final BookingService bookingService;

    /**
     * API: POST /api/v1/bookings Reserves seats and places the booking in a PENDING state with a
     * 10-minute lock.
     */
    @Operation(
            summary = "Reserve Seats",
            description =
                    "Acquires a distributed lock and creates a PENDING booking (10 min hold).")
    @ApiResponse(
            responseCode = "202",
            description = "Seats reserved and booking created in PENDING state.")
    @ApiResponse(responseCode = "409", description = "Concurrency conflict or seat already taken.")
    @PostMapping
    public ResponseEntity<BookingResponse> reserveSeats(
            @Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.reserveSeats(request);
        // 202 Accepted status indicates the request has been accepted for processing.
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * API: POST /api/v1/bookings/{id}/confirm Confirms a PENDING booking after successful payment.
     */
    @Operation(
            summary = "Confirm Booking",
            description = "Moves a PENDING booking to CONFIRMED state after payment success.")
    @ApiResponse(responseCode = "200", description = "Booking successfully confirmed.")
    @ApiResponse(responseCode = "404", description = "Booking not found.")
    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable("id") String bookingId,
            @RequestParam("paymentTransactionId") String paymentTransactionId) {

        BookingResponse response = bookingService.confirmBooking(bookingId, paymentTransactionId);
        return ResponseEntity.ok(response);
    }

    /**
     * API: DELETE /api/v1/bookings/{id}/cancel Cancels a CONFIRMED booking and releases seats back
     * to inventory.
     */
    @Operation(
            summary = "Cancel Booking",
            description = "Cancels a CONFIRMED booking and releases reserved seats.")
    @ApiResponse(responseCode = "204", description = "Booking successfully cancelled.")
    @ApiResponse(responseCode = "404", description = "Booking not found.")
    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable("id") String bookingId) {
        bookingService.cancelBooking(bookingId);
        // 204 No Content status is appropriate for successful deletion/cancellation.
        return ResponseEntity.noContent().build();
    }
}
