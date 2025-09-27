package com.busbooking.domains.dto.admin;

import com.busbooking.entities.Bus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value // Immutable DTO for responses
@Builder
public class BusResponseDto {
    @Schema(description = "The generated primary key ID of the bus.")
    private Long busId;

    @Schema(description = "The fleet operator.")
    private String operator;

    @Schema(description = "The total seating capacity.")
    private Integer totalSeats;

    @Schema(description = "Status message.")
    private String status = "SUCCESS";

    public static BusResponseDto fromEntity(Bus bus) {
        return BusResponseDto.builder()
                .busId(bus.getId())
                .operator(bus.getOperator())
                .totalSeats(bus.getTotalSeats())
                .build();
    }
}
