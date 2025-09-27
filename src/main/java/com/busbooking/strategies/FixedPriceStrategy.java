package com.busbooking.strategies;

import com.busbooking.entities.Booking;
import com.busbooking.entities.Seat;
import java.math.BigDecimal;
import java.util.List;

/** Strategy Pattern Implementation: Fixed Pricing (Base Price * Multiplier). */
class FixedPriceStrategy implements PricingStrategy {
    @Override
    public BigDecimal calculatePrice(Booking booking, List<Seat> seats) {
        BigDecimal baseFare = booking.getSchedule().getBasePrice();

        // In a real system, the base fare is for the full route. We calculate segment proportion.
        // Simplified approach: Base price * Seat Multiplier * Number of seats
        BigDecimal totalSeatPrice = BigDecimal.ZERO;
        for (Seat seat : seats) {
            totalSeatPrice = totalSeatPrice.add(baseFare.multiply(seat.getMultiplier()));
        }
        return totalSeatPrice;
    }
}
