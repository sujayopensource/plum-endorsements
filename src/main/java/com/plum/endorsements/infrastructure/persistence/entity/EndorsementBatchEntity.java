package com.plum.endorsements.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "endorsement_batches")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EndorsementBatchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "insurer_id", nullable = false)
    private UUID insurerId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "endorsement_count", nullable = false)
    private int endorsementCount;

    @Column(name = "total_premium")
    private BigDecimal totalPremium;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "insurer_batch_ref", length = 100)
    private String insurerBatchRef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_data", columnDefinition = "jsonb")
    private String responseData;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
