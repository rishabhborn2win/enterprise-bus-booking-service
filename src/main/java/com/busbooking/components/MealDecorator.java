package com.busbooking.components;

import java.math.BigDecimal;

/** Concrete Decorator 2: Adds Meal Fee */
public class MealDecorator extends AddonDecorator {
    private static final BigDecimal MEAL_PRICE = BigDecimal.valueOf(250.00);

    public MealDecorator(BookingComponent wrappee) {
        super(wrappee);
    }

    @Override
    public BigDecimal calculatePrice() {
        return wrappee.calculatePrice().add(MEAL_PRICE);
    }
}
