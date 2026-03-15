package com.plum.endorsements.api.dto;

import com.plum.endorsements.domain.model.EndorsementBatch;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BatchProgressResponse(
        UUID batchId,
        UUID insurerId,
        String status,
        int endorsementCount,
        BigDecimal totalPremium,
        Instant submittedAt,
        Instant slaDeadline,
        String insurerBatchRef,
        Instant createdAt
) {

    public static BatchProgressResponse from(EndorsementBatch batch) {
        return new BatchProgressResponse(
                batch.getId(),
                batch.getInsurerId(),
                batch.getStatus() != null ? batch.getStatus().name() : null,
                batch.getEndorsementCount(),
                batch.getTotalPremium(),
                batch.getSubmittedAt(),
                batch.getSlaDeadline(),
                batch.getInsurerBatchRef(),
                batch.getCreatedAt()
        );
    }
}
