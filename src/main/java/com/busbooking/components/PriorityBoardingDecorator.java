package com.busbooking.components;

import java.math.BigDecimal;

/**
 * Concrete Decorator 3: Adds Priority Boarding Fee
 */
public class PriorityBoardingDecorator extends AddonDecorator {
    private static final BigDecimal PRIORITY_FEE = BigDecimal.valueOf(100.00);

    public PriorityBoardingDecorator(BookingComponent wrappee) {
        super(wrappee);
    }

    @Override
    public BigDecimal calculatePrice() {
        return wrappee.calculatePrice().add(PRIORITY_FEE);
    }
}
