package com.busbooking.schedulers;

import com.busbooking.entities.Booking;
import com.busbooking.enums.BookingStatus;
import com.busbooking.repositories.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service for automatic cleanup of expired PENDING bookings.
 * This restores seat inventory if a user fails to confirm payment within the 10-minute window.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupScheduler {

    private final BookingRepository bookingRepository;
    // In a real service, the seat release would involve sending a message to the Search/Inventory service
    // or deleting the Redis lock/cache entry for the seat.

    /**
     * CRITICAL: Runs every minute to check for expired bookings.
     * The cron expression "0 * * * * *" means: at second 0 of every minute.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void cleanupExpiredBookings() {
        log.info("Starting scheduled cleanup of expired PENDING bookings...");
        
        // Find all bookings that are PENDING and have an expiration time in the past
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingRepository.findByStatusAndExpirationTimeBefore(
                BookingStatus.PENDING, now);

        if (expiredBookings.isEmpty()) {
            log.info("No expired PENDING bookings found to cleanup.");
            return;
        }

        log.warn("Found {} expired PENDING bookings. Processing cleanup...", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            try {
                // 1. Update status to EXPIRED in MySQL
                booking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);

                // 2. LOG: Seat inventory restoration (conceptual in this microservice)
                // In a real system, we'd delete the corresponding seat reservation from the Redis BookingCache here.
                log.info("Booking ID: {} EXPIRED. Seats are now considered released for re-booking.", booking.getId());

            } catch (Exception e) {
                // Structured Logging for failure
                log.error("Failed to cleanup expired booking ID: {}. Error: {}", booking.getId(), e.getMessage());
            }
        }
        log.info("Finished scheduled cleanup.");
    }
}
