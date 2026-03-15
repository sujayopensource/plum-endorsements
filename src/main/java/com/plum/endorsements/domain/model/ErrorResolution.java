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
public class ErrorResolution {

    private UUID id;
    private UUID endorsementId;
    private String errorType;
    private String originalValue;
    private String correctedValue;
    private String resolution;
    private double confidence;
    private boolean autoApplied;
    private Instant createdAt;
    private String outcome;
    private Instant outcomeAt;
    private String outcomeEndorsementStatus;

    public boolean shouldAutoApply(double threshold) {
        return confidence >= threshold;
    }

    public void recordOutcome(String outcome, String endorsementStatus) {
        this.outcome = outcome;
        this.outcomeAt = Instant.now();
        this.outcomeEndorsementStatus = endorsementStatus;
    }
}
