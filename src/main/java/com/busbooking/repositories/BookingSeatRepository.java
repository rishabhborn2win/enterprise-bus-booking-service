package com.busbooking.repositories;

import com.busbooking.entities.BookingSeat;
import com.busbooking.enums.BookingStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Repository for fetching seat availability and schedule segments. */
@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    /**
     * Checks if a seat is reserved for any segment that overlaps with the requested segment. This
     * query is complex and would be optimized in a real scenario, but conceptually, it ensures
     * multi-hop inventory safety.
     *
     * <p>In a simplified scenario, we just check for CONFIRMED bookings for that seat. The
     * concurrency check relies heavily on Redis locking.
     */
    boolean existsBySeatIdAndBooking_Status(Long seatId, BookingStatus status);

    List<BookingSeat> findAllBySeatId(Long seatId);

    // Fetch all confirmed seat bookings and their segment orders for a given schedule.
    // The query includes joins to get the actual stop_order from the ScheduleStop table.
    @Query(
            nativeQuery = true,
            value =
                    "SELECT "
                            + "    bs.seat_id, "
                            + "    ss_start.stop_order AS booked_start_order, "
                            + "    ss_end.stop_order AS booked_end_order "
                            + "FROM booking_seat bs "
                            + "JOIN booking b ON bs.booking_id = b.id "
                            + "JOIN schedule_stop ss_start ON b.schedule_id = ss_start.schedule_id AND bs.segment_start_stop_id = ss_start.stop_id "
                            + "JOIN schedule_stop ss_end ON b.schedule_id = ss_end.schedule_id AND bs.segment_end_stop_id = ss_end.stop_id "
                            + "WHERE b.schedule_id = :scheduleId AND b.status = 'CONFIRMED'")
    List<Object[]> findConfirmedBookedSeatSegmentsWithOrders(Long scheduleId);
}
