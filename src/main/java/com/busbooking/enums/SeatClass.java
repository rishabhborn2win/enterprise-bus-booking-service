package com.busbooking.enums;

import lombok.Getter; /**
 * Defines the various seat classes with their base pricing multipliers.
 */
@Getter
public enum SeatClass {
    SEATING(1.0),
    AC_SEATING(1.5),
    SLEEPER(2.5),
    AC_SLEEPER(4.0),
    AC_SLEEPER_AND_SEATER(2.0); // Mid-range option for mixed buses

    private final double multiplier;

    SeatClass(double multiplier) {
        this.multiplier = multiplier;
    }
}
