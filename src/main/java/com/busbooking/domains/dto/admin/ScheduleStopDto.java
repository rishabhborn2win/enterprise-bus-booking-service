package com.busbooking.domains.dto.admin;

import java.time.LocalDateTime;
import lombok.Data;

@Data
class ScheduleStopDto {
    private Long stopId;
    private Integer stopOrder;
    private LocalDateTime arrivalTime;
}
