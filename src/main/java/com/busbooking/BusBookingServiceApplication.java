package com.busbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Bus Booking Service.
 * Enables Spring Boot features, scheduling for cleanup, and JPA.
 */
@SpringBootApplication
@EnableScheduling // Enables the BookingCleanupScheduler (10-min expiration logic)
public class BusBookingServiceApplication {

    /**
     * Professional Logging Standard: Using main method for application launch log.
     */
    public static void main(String[] args) {
        // Structured Logging: Informative start
        System.out.println("Starting Bus Booking Service...");
        SpringApplication.run(BusBookingServiceApplication.class, args);
        System.out.println("Bus Booking Service is running! Access Swagger UI at: http://localhost:8080/swagger-ui.html");
    }
}
