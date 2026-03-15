package com.plum.endorsements.api.dto;

import com.plum.endorsements.domain.model.ProcessMiningMetric;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProcessMiningMetricResponse(
        UUID id,
        UUID insurerId,
        String fromStatus,
        String toStatus,
        long avgDurationMs,
        long p95DurationMs,
        long p99DurationMs,
        int sampleCount,
        BigDecimal happyPathPct,
        Instant calculatedAt
) {
    public static ProcessMiningMetricResponse from(ProcessMiningMetric metric) {
        return new ProcessMiningMetricResponse(
                metric.getId(),
                metric.getInsurerId(),
                metric.getFromStatus(),
                metric.getToStatus(),
                metric.getAvgDurationMs(),
                metric.getP95DurationMs(),
                metric.getP99DurationMs(),
                metric.getSampleCount(),
                metric.getHappyPathPct(),
                metric.getCalculatedAt()
        );
    }
}
