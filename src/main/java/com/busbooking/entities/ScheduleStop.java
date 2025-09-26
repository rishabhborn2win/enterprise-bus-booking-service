package com.busbooking.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Defines an intermediate or terminal stop for a specific schedule.
 * CRITICAL for Multi-hop logic.
 * Maps to the SCHEDULE_STOP table.
 */
@Entity
@Table(name = "schedule_stop")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)

public class ScheduleStop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    @ToString.Exclude
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    @ToString.Exclude
    private Stop stop;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder; // Sequence in the journey (1, 2, 3...)

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;
}
