package com.busbooking.components;

import java.math.BigDecimal;

/**
 * Decorator Pattern: Component Interface
 * Defines the contract for calculating the price of a booking item.
 */
public interface BookingComponent {
    /**
     * Calculates the cumulative price of the booking and its decorated components.
     */
    BigDecimal calculatePrice();
}

