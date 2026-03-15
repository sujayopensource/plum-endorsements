package com.plum.endorsements.api.dto;

import com.plum.endorsements.domain.model.ReconciliationRun;

import java.time.Instant;
import java.util.UUID;

public record ReconciliationRunResponse(
        UUID id,
        UUID insurerId,
        String status,
        int totalChecked,
        int matched,
        int partialMatched,
        int rejected,
        int missing,
        Instant startedAt,
        Instant completedAt
) {
    public static ReconciliationRunResponse from(ReconciliationRun run) {
        return new ReconciliationRunResponse(
                run.getId(), run.getInsurerId(), run.getStatus(),
                run.getTotalChecked(), run.getMatched(), run.getPartialMatched(),
                run.getRejected(), run.getMissing(),
                run.getStartedAt(), run.getCompletedAt()
        );
    }
}
