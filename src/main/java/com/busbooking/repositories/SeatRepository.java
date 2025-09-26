package com.busbooking.repositories;

import com.busbooking.entities.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; /**
 * Simple repositories for static entities needed for foreign key relationships.
 */
@Repository public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByScheduleId(Long scheduleId);
    Optional<Seat> findByScheduleIdAndSeatNumber(Long scheduleId, String seatNumber);
}
