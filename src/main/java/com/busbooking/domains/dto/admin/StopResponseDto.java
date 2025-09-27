package com.busbooking.domains.dto.admin;

import com.busbooking.entities.Stop;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StopResponseDto {
    private Long stopId;
    private String name;
    private String city;
    private String status = "SUCCESS";

    public static StopResponseDto fromEntity(Stop stop) {
        return StopResponseDto.builder()
                .stopId(stop.getId())
                .name(stop.getName())
                .city(stop.getCity())
                .build();
    }
}
