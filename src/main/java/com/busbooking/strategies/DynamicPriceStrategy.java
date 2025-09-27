package com.busbooking.strategies;

import com.busbooking.entities.Booking;
import com.busbooking.entities.Seat;
import java.math.BigDecimal;
import java.util.List;

/**
 * Strategy Pattern Implementation: Dynamic Pricing Engine. Adds a demand factor (1.0x to 1.5x)
 * based on current availability.
 */
public class DynamicPriceStrategy implements PricingStrategy {

    private final int totalSeats;
    private final int
            reservedSeats; // This would come from a real-time inventory service (Search Service)

    public DynamicPriceStrategy(int totalSeats, int reservedSeats) {
        this.totalSeats = totalSeats;
        this.reservedSeats = reservedSeats;
    }

    @Override
    public BigDecimal calculatePrice(Booking booking, List<Seat> seats) {
        // Start with the fixed price calculation
        BigDecimal fixedPrice = new FixedPriceStrategy().calculatePrice(booking, seats);

        // Dynamic Pricing Logic: Demand-based factor
        double soldPercentage = (double) reservedSeats / totalSeats;

        // Demand Factor: Scales from 1.0 (low demand) up to 1.5 (high demand)
        // Max factor of 1.5 is hit when soldPercentage is 1.0 (fully booked)
        double demandFactor = 1.0 + (0.5 * soldPercentage);

        return fixedPrice
                .multiply(BigDecimal.valueOf(demandFactor))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
