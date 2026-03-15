package com.plum.endorsements.api.dto;

import com.plum.endorsements.domain.model.AnomalyDetection;

import java.time.Instant;
import java.util.UUID;

public record AnomalyDetectionResponse(
        UUID id,
        UUID endorsementId,
        UUID employerId,
        String anomalyType,
        double score,
        String severity,
        String explanation,
        Instant flaggedAt,
        Instant reviewedAt,
        String status,
        String reviewerNotes
) {
    public static AnomalyDetectionResponse from(AnomalyDetection anomaly) {
        String severity = anomaly.getScore() >= 0.8 ? "HIGH"
                : anomaly.getScore() >= 0.6 ? "MEDIUM" : "LOW";
        return new AnomalyDetectionResponse(
                anomaly.getId(),
                anomaly.getEndorsementId(),
                anomaly.getEmployerId(),
                anomaly.getAnomalyType().name(),
                anomaly.getScore(),
                severity,
                anomaly.getExplanation(),
                anomaly.getFlaggedAt(),
                anomaly.getReviewedAt(),
                anomaly.getStatus().name(),
                anomaly.getReviewerNotes()
        );
    }
}
