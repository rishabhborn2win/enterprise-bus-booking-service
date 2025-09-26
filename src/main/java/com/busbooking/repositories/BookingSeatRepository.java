package com.busbooking.repositories;

import com.busbooking.entities.BookingSeat;
import com.busbooking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; /**
 * Repository for fetching seat availability and schedule segments.
 */
@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    /**
     * Checks if a seat is reserved for any segment that overlaps with the requested segment.
     * This query is complex and would be optimized in a real scenario, but conceptually,
     * it ensures multi-hop inventory safety.
     *
     * In a simplified scenario, we just check for CONFIRMED bookings for that seat.
     * The concurrency check relies heavily on Redis locking.
     */
    boolean existsBySeatIdAndBooking_Status(Long seatId, BookingStatus status);
}
