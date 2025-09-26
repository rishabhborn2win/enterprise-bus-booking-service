package com.busbooking.configurations;

import com.busbooking.strategies.DynamicPriceStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class dedicated to setting up Pricing Strategy beans.
 * Isolating this bean definition helps Spring resolve complex dependency graphs
 * and avoids circular dependency issues with the BookingService.
 */
@Configuration
public class PricingConfig {

    /**
     * Defines the DynamicPriceStrategy bean.
     * NOTE: In a production environment, 'totalSeats' and 'seatsSold' would
     * be fetched dynamically from the Search/Inventory Service via API or database.
     * This implementation uses mock data (45 total seats, 10 currently reserved)
     * as required by the prompt's focus on the Booking Service logic.
     *
     * @return The configured DynamicPriceStrategy instance.
     */
    @Bean
    public DynamicPriceStrategy dynamicPricingStrategy() {
        // Mock inventory data for dynamic pricing calculation
        return new DynamicPriceStrategy(45, 10);
    }
}
