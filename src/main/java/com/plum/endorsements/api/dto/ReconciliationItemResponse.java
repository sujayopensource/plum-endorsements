package com.plum.endorsements.api.dto;

import com.plum.endorsements.domain.model.ReconciliationItem;

import java.time.Instant;
import java.util.UUID;

public record ReconciliationItemResponse(
        UUID id,
        UUID runId,
        UUID endorsementId,
        UUID batchId,
        UUID insurerId,
        UUID employerId,
        String outcome,
        String actionTaken,
        Instant createdAt
) {
    public static ReconciliationItemResponse from(ReconciliationItem item) {
        return new ReconciliationItemResponse(
                item.getId(), item.getRunId(), item.getEndorsementId(),
                item.getBatchId(), item.getInsurerId(), item.getEmployerId(),
                item.getOutcome().name(), item.getActionTaken(), item.getCreatedAt()
        );
    }
}
