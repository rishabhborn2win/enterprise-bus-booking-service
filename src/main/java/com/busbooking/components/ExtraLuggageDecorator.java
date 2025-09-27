package com.busbooking.components;

import java.math.BigDecimal;

/** Concrete Decorator 1: Adds Extra Luggage Fee */
public class ExtraLuggageDecorator extends AddonDecorator {
    private static final BigDecimal LUGGAGE_FEE = BigDecimal.valueOf(150.00);

    public ExtraLuggageDecorator(BookingComponent wrappee) {
        super(wrappee);
    }

    @Override
    public BigDecimal calculatePrice() {
        return wrappee.calculatePrice().add(LUGGAGE_FEE);
    }
}
