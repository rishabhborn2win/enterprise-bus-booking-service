package com.busbooking.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Entity linking a specific seat to a specific booked segment within a booking. CRITICAL for
 * multi-hop inventory management.
 */
@Entity
@Table(name = "booking_seat")
@Data
@ToString(exclude = "booking")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BookingSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    // Segment definition for multi-hop
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_start_stop_id", nullable = false)
    private com.busbooking.entities.Stop segmentStartStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_end_stop_id", nullable = false)
    private Stop segmentEndStop;
}
