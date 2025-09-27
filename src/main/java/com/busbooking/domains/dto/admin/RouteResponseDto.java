package com.busbooking.domains.dto.admin;

import com.busbooking.entities.Route;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value // Immutable DTO for responses
@Builder
public class RouteResponseDto {
    @Schema(description = "The generated primary key ID of the Route.")
    private Long routeId;

    @Schema(description = "Name of the source city/stop.")
    private String source;

    @Schema(description = "Name of the destination city/stop.")
    private String destination;

    @Schema(description = "Status message.")
    private String status = "SUCCESS";

    public static RouteResponseDto fromEntity(Route route) {
        return RouteResponseDto.builder()
                .routeId(route.getId())
                .source(route.getSourceStop().getName()) // Requires Stop entity to be loaded
                .destination(
                        route.getDestinationStop().getName()) // Requires Stop entity to be loaded
                .build();
    }
}
