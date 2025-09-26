package com.busbooking.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

/**
 * Entity for the physical bus vehicle.
 * Maps to the BUS table.
 */
@Entity
@Table(name = "bus")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Bus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number", unique = true, nullable = false)
    private String registrationNumber;

    @Column(name = "operator", nullable = false)
    private String operator; // e.g., KSRTC, UPSRTC, Royal Buses

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @OneToMany(mappedBy = "bus", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Schedule> schedules;
}

