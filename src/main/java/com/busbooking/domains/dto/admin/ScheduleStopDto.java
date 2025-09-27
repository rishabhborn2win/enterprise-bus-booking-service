package com.busbooking.domains.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
class ScheduleStopDto {
    private Long stopId;
    private Integer stopOrder;
    private LocalDateTime arrivalTime;
}
