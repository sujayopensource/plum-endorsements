package com.plum.endorsements.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "endorsement_id", nullable = false)
    private UUID endorsementId;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "insurer_id", nullable = false)
    private UUID insurerId;

    @Column(name = "employer_id", nullable = false)
    private UUID employerId;

    @Column(nullable = false, length = 20)
    private String outcome;

    @Column(name = "sent_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String sentData;

    @Column(name = "confirmed_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String confirmedData;

    @Column(name = "discrepancy_details", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String discrepancyDetails;

    @Column(name = "action_taken", length = 100)
    private String actionTaken;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
