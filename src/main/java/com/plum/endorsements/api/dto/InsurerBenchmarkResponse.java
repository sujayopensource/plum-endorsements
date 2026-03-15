package com.plum.endorsements.api.dto;

import com.plum.endorsements.application.service.InsurerBenchmarkService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InsurerBenchmarkResponse(
        UUID insurerId,
        String insurerName,
        String insurerCode,
        long avgProcessingMs,
        long p95ProcessingMs,
        long p99ProcessingMs,
        BigDecimal stpRate,
        int totalSamples,
        Instant calculatedAt
) {
    public static InsurerBenchmarkResponse from(InsurerBenchmarkService.InsurerBenchmark benchmark) {
        return new InsurerBenchmarkResponse(
                benchmark.insurerId(),
                benchmark.insurerName(),
                benchmark.insurerCode(),
                benchmark.avgProcessingMs(),
                benchmark.p95ProcessingMs(),
                benchmark.p99ProcessingMs(),
                benchmark.stpRate(),
                benchmark.totalSamples(),
                benchmark.calculatedAt()
        );
    }
}
