package com.busbooking.components;

import java.math.BigDecimal;

/**
 * Decorator Pattern: Concrete Component (Base Price)
 */
public class BasicBooking implements BookingComponent {
    private final BigDecimal basePrice;

    public BasicBooking(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    @Override
    public BigDecimal calculatePrice() {
        return basePrice;
    }
}
