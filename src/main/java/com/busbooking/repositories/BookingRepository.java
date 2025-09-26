package com.busbooking.repositories;

import com.busbooking.entities.Booking;
import com.busbooking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for the Booking Entity.
 * Implements the Repository Pattern for clean data access.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    /**
     * Finds bookings that are PENDING and have expired.
     * CRITICAL for the automatic expiration and seat release logic.
     */
    List<Booking> findByStatusAndExpirationTimeBefore(BookingStatus status, java.time.LocalDateTime cutoffTime);
}

