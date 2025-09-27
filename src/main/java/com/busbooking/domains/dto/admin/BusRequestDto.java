package com.busbooking.domains.dto.admin; // ** In: com.busbooking.domains.dto.admin.BusRequestDto

// **

import com.busbooking.entities.Bus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BusRequestDto {
    @Schema(description = "The unique registration number (e.g., KA01AB1234).")
    @NotBlank
    private String registrationNumber;

    @Schema(description = "The bus operator's name (e.g., VRL, KSRTC).")
    @NotBlank
    private String operator;

    @Schema(description = "Total number of seats/sleepers on the bus.")
    @NotNull
    private Integer totalSeats;

    public Bus toEntity() {
        return toEntity(null);
    }

    // Method to handle both POST (new) and PUT (update)
    public Bus toEntity(Long id) {
        Bus bus = new Bus();
        bus.setId(id);
        bus.setRegistrationNumber(this.registrationNumber);
        bus.setOperator(this.operator);
        bus.setTotalSeats(this.totalSeats);
        return bus;
    }
}
