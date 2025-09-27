package com.busbooking.entities;

import com.busbooking.enums.BookingStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

/**
 * JPA Entity representing a single booking transaction. Tracks the booking state, price, and the
 * specific segment of the schedule booked.
 */
@Entity
@Table(name = "booking")
@Data
@NoArgsConstructor
@ToString(exclude = "reservedSeats")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Booking {

    /** Using UUID for enterprise-grade, non-sequential booking ID */
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_stop_id", nullable = false)
    private Stop startStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_stop_id", nullable = false)
    private Stop endStop;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @Column(name = "final_price", nullable = false)
    private BigDecimal finalPrice;

    @Column(name = "booking_time", nullable = false)
    private LocalDateTime bookingTime;

    /** CRITICAL: Used by the scheduler for automatic cleanup and seat release */
    @Column(name = "expiration_time", nullable = false)
    private LocalDateTime expirationTime;

    /** Multi-hop support: One booking can reserve multiple segments of seats */
    @OneToMany(
            mappedBy = "booking",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<BookingSeat> reservedSeats = new HashSet<>();

    /** Decorator Pattern: Booking addons (extra luggage, meal, etc.) */
    @ManyToMany
    @JoinTable(
            name = "booking_addon",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "addon_id"))
    private Set<Addon> addons = new HashSet<>();
}
