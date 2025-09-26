package com.busbooking.components;

import java.math.BigDecimal;

/**
 * Decorator Pattern: Decorator Abstract Class
 */
abstract class AddonDecorator implements BookingComponent {
    protected BookingComponent wrappee;

    public AddonDecorator(BookingComponent wrappee) {
        this.wrappee = wrappee;
    }

    // Default implementation uses the wrapped component's price
    @Override
    public BigDecimal calculatePrice() {
        return wrappee.calculatePrice();
    }
}
