package com.plum.endorsements.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "stp_rate_snapshots")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StpRateSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "insurer_id", nullable = false)
    private UUID insurerId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_endorsements", nullable = false)
    @Builder.Default
    private int totalEndorsements = 0;

    @Column(name = "stp_endorsements", nullable = false)
    @Builder.Default
    private int stpEndorsements = 0;

    @Column(name = "stp_rate", nullable = false, columnDefinition = "DECIMAL(5,4)")
    @Builder.Default
    private BigDecimal stpRate = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
