package com.plum.endorsements.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "process_mining_metrics")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProcessMiningMetricEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "insurer_id", nullable = false)
    private UUID insurerId;

    @Column(name = "from_status", nullable = false, length = 50)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 50)
    private String toStatus;

    @Column(name = "avg_duration_ms", nullable = false)
    private long avgDurationMs;

    @Column(name = "p95_duration_ms", nullable = false)
    private long p95DurationMs;

    @Column(name = "p99_duration_ms", nullable = false)
    private long p99DurationMs;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Column(name = "happy_path_pct")
    private BigDecimal happyPathPct;

    @Column(name = "calculated_at", nullable = false)
    @Builder.Default
    private Instant calculatedAt = Instant.now();
}
