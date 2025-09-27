package com.busbooking.repositories;

import com.busbooking.entities.Schedule;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query(
            nativeQuery = true,
            value =
                    "SELECT s.id "
                            + "FROM schedule s "
                            +
                            // Use an inner join derived table to find the final stop time for ALL
                            // schedules once
                            "JOIN ("
                            + "SELECT ss_max.schedule_id, ss_max.arrival_time AS final_arrival_time "
                            + "FROM schedule_stop ss_max "
                            + "INNER JOIN ("
                            +
                            // Subquery to find the MAX stop order for every schedule
                            "SELECT schedule_id, MAX(stop_order) AS max_order "
                            + "FROM schedule_stop GROUP BY schedule_id"
                            + ") AS ss_max_order ON ss_max.schedule_id = ss_max_order.schedule_id AND ss_max.stop_order = ss_max_order.max_order "
                            + ") AS final_stop ON s.id = final_stop.schedule_id "
                            +

                            // --- Filtering and Overlap Check ---
                            "WHERE s.bus_id = :busId "
                            + "AND s.id <> :scheduleIdToExclude "
                            +

                            // Overlap Condition: (Existing Start < New End) AND (Existing End > New
                            // Start)
                            "AND s.departure_time < :newScheduleTerminationTime "
                            + "AND final_stop.final_arrival_time > :newScheduleStartTime")
    List<Long> findConflictingSchedules(
            @Param("busId") Long busId,
            @Param("scheduleIdToExclude") Long scheduleIdToExclude,
            @Param("newScheduleTerminationTime") LocalDateTime newScheduleTerminationTime,
            @Param("newScheduleStartTime") LocalDateTime newScheduleStartTime);

    List<Schedule> findByBusId(Long busId);
}
