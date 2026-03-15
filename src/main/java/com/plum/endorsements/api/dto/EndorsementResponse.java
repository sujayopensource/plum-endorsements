package com.plum.endorsements.api.dto;

import com.plum.endorsements.domain.model.Endorsement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EndorsementResponse(
        UUID id,
        UUID employerId,
        UUID employeeId,
        UUID insurerId,
        UUID policyId,
        String type,
        String status,
        LocalDate coverageStartDate,
        LocalDate coverageEndDate,
        BigDecimal premiumAmount,
        UUID batchId,
        String insurerReference,
        int retryCount,
        String failureReason,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt
) {

    public static EndorsementResponse from(Endorsement e) {
        return new EndorsementResponse(
                e.getId(),
                e.getEmployerId(),
                e.getEmployeeId(),
                e.getInsurerId(),
                e.getPolicyId(),
                e.getType() != null ? e.getType().name() : null,
                e.getStatus() != null ? e.getStatus().name() : null,
                e.getCoverageStartDate(),
                e.getCoverageEndDate(),
                e.getPremiumAmount(),
                e.getBatchId(),
                e.getInsurerReference(),
                e.getRetryCount(),
                e.getFailureReason(),
                e.getIdempotencyKey(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
