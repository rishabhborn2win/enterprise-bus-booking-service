package com.busbooking.strategies;

import com.busbooking.entities.Booking;
import com.busbooking.entities.Seat;
import java.math.BigDecimal;
import java.util.List;

/** Strategy Pattern Interface: Defines the contract for all pricing algorithms. */
public interface PricingStrategy {
    /**
     * Calculates the final price for a given booking request.
     *
     * @param booking The pending booking entity containing base details.
     * @param seats The list of seats selected.
     * @return The final calculated price.
     */
    BigDecimal calculatePrice(Booking booking, List<Seat> seats);
}
