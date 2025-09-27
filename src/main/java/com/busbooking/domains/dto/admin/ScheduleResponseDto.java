package com.busbooking.domains.dto.admin;

import com.busbooking.entities.Schedule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

// In: com.bussystem.dto.ScheduleResponseDto
@Data
@AllArgsConstructor
public class ScheduleResponseDto {

    @Schema(description = "Newly created Schedule ID.")
    private Long scheduleId;

    @Schema(description = "Status of the operation.")
    private String status = "SUCCESS";

    public ScheduleResponseDto(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public static ScheduleResponseDto fromEntity(Schedule schedule) {
        return new ScheduleResponseDto(schedule.getId());
    }
}
