package com.busbooking.enums;

/**
 * Defines the possible states of a booking transaction.
 */
public enum BookingStatus {
    PENDING,        // Initial state, seat is held (10 min expiration timer)
    CONFIRMED,      // Payment successful, ticket issued
    CANCELLED,      // User cancelled, seats released
    EXPIRED         // Timeout occurred, seats released
}

