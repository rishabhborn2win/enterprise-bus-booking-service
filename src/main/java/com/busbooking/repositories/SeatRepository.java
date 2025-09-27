package com.busbooking.repositories;

import com.busbooking.entities.Seat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Simple repositories for static entities needed for foreign key relationships. */
@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByScheduleId(Long scheduleId);

    Optional<Seat> findByScheduleIdAndSeatNumber(Long scheduleId, String seatNumber);
}
