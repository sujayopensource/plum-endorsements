package com.plum.endorsements.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "insurer_id", nullable = false)
    private UUID insurerId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "RUNNING";

    @Column(name = "total_checked", nullable = false)
    @Builder.Default
    private int totalChecked = 0;

    @Column(nullable = false)
    @Builder.Default
    private int matched = 0;

    @Column(name = "partial_matched", nullable = false)
    @Builder.Default
    private int partialMatched = 0;

    @Column(nullable = false)
    @Builder.Default
    private int rejected = 0;

    @Column(nullable = false)
    @Builder.Default
    private int missing = 0;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;
}
