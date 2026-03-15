package com.plum.endorsements.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessMiningMetric {

    private UUID id;
    private UUID insurerId;
    private String fromStatus;
    private String toStatus;
    private long avgDurationMs;
    private long p95DurationMs;
    private long p99DurationMs;
    private int sampleCount;
    private BigDecimal happyPathPct;
    private Instant calculatedAt;
}
