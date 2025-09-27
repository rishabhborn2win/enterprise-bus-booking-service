package com.busbooking.domains.dto.admin; // ** In: com.busbooking.domains.dto.admin.RouteRequestDto

// **

// (Assuming imports from BusRequestDto above are used)
// Note: This DTO is critical as it passes the Stop IDs to the AdminService.

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RouteRequestDto {
    @Schema(description = "ID of the Route (Optional for update).")
    private Long id;

    @Schema(description = "ID of the starting Stop.", required = true)
    @NotNull
    private Long sourceStopId;

    @Schema(description = "ID of the final Destination Stop.", required = true)
    @NotNull
    private Long destStopId;

    @Schema(description = "Distance in Kilometers for the direct route.", required = true)
    @NotNull
    private Integer distanceKm;

    // The 'toEntity' conversion logic is usually simplified here,
    // as the AdminService must fetch the actual Stop entities (StopRepository).
    // The Route entity relies on the Stop entities being attached by the service.
}
