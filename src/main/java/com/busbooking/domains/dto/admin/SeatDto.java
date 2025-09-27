package com.busbooking.domains.dto.admin;

import com.busbooking.enums.SeatClass;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class SeatDto {
    @Schema(description = "The unique number of the seat (e.g., A1, 12, S3).")
    private String seatNumber;

    @Schema(description = "The seating class (e.g., AC_SLEEPER, SEATER).")
    private SeatClass seatClass;

    @Schema(description = "Price multiplier (e.g., 1.0 for standard, 1.5 for premium).")
    private BigDecimal priceMultiplier;
}
