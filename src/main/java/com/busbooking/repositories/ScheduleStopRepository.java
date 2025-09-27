package com.busbooking.repositories;

import com.busbooking.entities.ScheduleStop;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleStopRepository extends JpaRepository<ScheduleStop, Integer> {
    /**
     * Finds the 'stopOrder' for a specific stop within a specific schedule. This is CRITICAL for
     * the segment-based availability calculation.
     */
    @Query(
            "SELECT ss.stopOrder FROM ScheduleStop ss "
                    + "WHERE ss.schedule.id = :scheduleId AND ss.stop.id = :stopId")
    Optional<Integer> findStopOrderByScheduleIdAndStopId(
            @Param("scheduleId") Long scheduleId, @Param("stopId") Long stopId);

    // --- Admin-related methods ---

    /** Deletes all stops associated with a schedule (used during update/delete operations). */
    @Modifying
    @Query("DELETE FROM ScheduleStop ss WHERE ss.schedule.id = :scheduleId")
    void deleteByScheduleId(@Param("scheduleId") Long scheduleId);

    /** Finds all stops for a schedule (used in SyncService mapping). */
    List<ScheduleStop> findByScheduleId(Long scheduleId);
}
