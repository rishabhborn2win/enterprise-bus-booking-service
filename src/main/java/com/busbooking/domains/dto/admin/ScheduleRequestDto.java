package com.busbooking.domains.dto.admin;

import com.busbooking.entities.Schedule;
import com.busbooking.entities.ScheduleStop;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

// In: com.bussystem.dto.ScheduleRequestDto
@Data
public class ScheduleRequestDto {

    @Schema(description = "ID of the Bus associated with this schedule.")
    private Long busId;

    @Schema(description = "ID of the Route (Source->Destination).")
    private Long routeId;

    @Schema(description = "Full departure date and time (YYYY-MM-DDTHH:MM:SS).")
    private LocalDateTime departureTime;

    @Schema(description = "Base fare for the full route.")
    private BigDecimal basePrice;

    @Schema(description = "List of stops and their sequence/timing for this schedule.")
    private List<ScheduleStopDto> stops;

    // --- Conversion Methods (Implementation needed) ---
    public Schedule toScheduleEntity() {
        /* ... conversion logic ... */
        return new Schedule();
    }

    public List<ScheduleStop> toScheduleStopEntities(Schedule schedule) {
        /* ... conversion logic ... */
        return List.of();
    }
}
