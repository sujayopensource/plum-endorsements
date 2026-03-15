package com.plum.endorsements.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDetection {

    private UUID id;
    private UUID endorsementId;
    private UUID employerId;
    private AnomalyType anomalyType;
    private double score;
    private String explanation;
    private Instant flaggedAt;
    private Instant reviewedAt;
    private AnomalyStatus status;
    private String reviewerNotes;

    public void review(AnomalyStatus newStatus, String notes) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition anomaly from " + status + " to " + newStatus);
        }
        this.status = newStatus;
        this.reviewedAt = Instant.now();
        this.reviewerNotes = notes;
    }
}
