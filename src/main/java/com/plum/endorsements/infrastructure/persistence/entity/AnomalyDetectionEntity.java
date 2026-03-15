package com.plum.endorsements.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "anomaly_detections")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AnomalyDetectionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "endorsement_id")
    private UUID endorsementId;

    @Column(name = "employer_id", nullable = false)
    private UUID employerId;

    @Column(name = "anomaly_type", nullable = false, length = 50)
    private String anomalyType;

    @Column(nullable = false, columnDefinition = "numeric(5,4)")
    private double score;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "flagged_at", nullable = false)
    @Builder.Default
    private Instant flaggedAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "FLAGGED";

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;
}
