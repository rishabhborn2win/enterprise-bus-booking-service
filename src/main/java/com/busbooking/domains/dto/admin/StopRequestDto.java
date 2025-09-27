package com.busbooking.domains.dto.admin; // ** In: com.busbooking.domains.dto.admin.StopRequestDto

// **

import com.busbooking.entities.Stop;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StopRequestDto {
    @Schema(description = "Name of the stop (e.g., Pune Central Bus Stand).")
    @NotBlank
    private String name;

    @Schema(description = "City where the stop is located (e.g., Pune).")
    @NotBlank
    private String city;

    public Stop toEntity() {
        Stop stop = new Stop();
        stop.setName(this.name);
        stop.setCity(this.city);
        return stop;
    }
}
