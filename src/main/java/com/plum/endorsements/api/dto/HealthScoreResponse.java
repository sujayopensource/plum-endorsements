package com.plum.endorsements.api.dto;

import com.plum.endorsements.application.service.EmployerHealthScoreService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HealthScoreResponse(
        UUID employerId,
        BigDecimal overallScore,
        String riskLevel,
        BigDecimal endorsementSuccessRate,
        BigDecimal anomalyScore,
        BigDecimal balanceHealthScore,
        BigDecimal reconciliationScore,
        Instant calculatedAt
) {
    public static HealthScoreResponse from(EmployerHealthScoreService.HealthScore score) {
        return new HealthScoreResponse(
                score.employerId(),
                score.overallScore(),
                score.riskLevel(),
                score.endorsementSuccessRate(),
                score.anomalyScore(),
                score.balanceHealthScore(),
                score.reconciliationScore(),
                score.calculatedAt()
        );
    }
}
