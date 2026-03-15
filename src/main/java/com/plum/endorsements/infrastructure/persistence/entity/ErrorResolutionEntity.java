package com.plum.endorsements.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "error_resolutions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ErrorResolutionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "endorsement_id")
    private UUID endorsementId;

    @Column(name = "error_type", nullable = false, length = 100)
    private String errorType;

    @Column(name = "original_value", columnDefinition = "TEXT")
    private String originalValue;

    @Column(name = "corrected_value", columnDefinition = "TEXT")
    private String correctedValue;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String resolution;

    @Column(nullable = false, columnDefinition = "numeric(5,4)")
    private double confidence;

    @Column(name = "auto_applied", nullable = false)
    @Builder.Default
    private boolean autoApplied = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "outcome", length = 20)
    private String outcome;

    @Column(name = "outcome_at")
    private Instant outcomeAt;

    @Column(name = "outcome_endorsement_status", length = 50)
    private String outcomeEndorsementStatus;
}
