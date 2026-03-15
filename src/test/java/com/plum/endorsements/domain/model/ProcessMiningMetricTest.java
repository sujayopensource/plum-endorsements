package com.plum.endorsements.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProcessMiningMetric domain model")
class ProcessMiningMetricTest {

    @Test
    @DisplayName("builder sets all fields correctly")
    void builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        UUID insurerId = UUID.randomUUID();
        Instant calculatedAt = Instant.now();

        ProcessMiningMetric metric = ProcessMiningMetric.builder()
                .id(id)
                .insurerId(insurerId)
                .fromStatus("CREATED")
                .toStatus("VALIDATED")
                .avgDurationMs(5000L)
                .p95DurationMs(12000L)
                .p99DurationMs(18000L)
                .sampleCount(150)
                .happyPathPct(new BigDecimal("85.50"))
                .calculatedAt(calculatedAt)
                .build();

        assertThat(metric.getId()).isEqualTo(id);
        assertThat(metric.getInsurerId()).isEqualTo(insurerId);
        assertThat(metric.getFromStatus()).isEqualTo("CREATED");
        assertThat(metric.getToStatus()).isEqualTo("VALIDATED");
        assertThat(metric.getAvgDurationMs()).isEqualTo(5000L);
        assertThat(metric.getP95DurationMs()).isEqualTo(12000L);
        assertThat(metric.getP99DurationMs()).isEqualTo(18000L);
        assertThat(metric.getSampleCount()).isEqualTo(150);
        assertThat(metric.getHappyPathPct()).isEqualByComparingTo(new BigDecimal("85.50"));
        assertThat(metric.getCalculatedAt()).isEqualTo(calculatedAt);
    }

    @Test
    @DisplayName("metric with null happyPathPct is valid")
    void builder_NullHappyPathPct_IsValid() {
        ProcessMiningMetric metric = ProcessMiningMetric.builder()
                .insurerId(UUID.randomUUID())
                .fromStatus("PROVISIONALLY_COVERED")
                .toStatus("SUBMITTED_REALTIME")
                .avgDurationMs(3000L)
                .p95DurationMs(8000L)
                .p99DurationMs(15000L)
                .sampleCount(50)
                .happyPathPct(null)
                .build();

        assertThat(metric.getHappyPathPct()).isNull();
    }

    @Test
    @DisplayName("metric captures high-latency transitions")
    void builder_HighLatencyTransition_CapturesValues() {
        ProcessMiningMetric metric = ProcessMiningMetric.builder()
                .insurerId(UUID.randomUUID())
                .fromStatus("BATCH_SUBMITTED")
                .toStatus("INSURER_PROCESSING")
                .avgDurationMs(3_600_000L) // 1 hour
                .p95DurationMs(14_400_000L) // 4 hours
                .p99DurationMs(28_800_000L) // 8 hours
                .sampleCount(200)
                .happyPathPct(new BigDecimal("72.00"))
                .calculatedAt(Instant.now())
                .build();

        assertThat(metric.getAvgDurationMs()).isEqualTo(3_600_000L);
        assertThat(metric.getP95DurationMs()).isGreaterThan(metric.getAvgDurationMs());
        assertThat(metric.getP99DurationMs()).isGreaterThan(metric.getP95DurationMs());
    }

    @Test
    @DisplayName("setter updates values correctly")
    void setter_UpdatesValues() {
        ProcessMiningMetric metric = ProcessMiningMetric.builder()
                .insurerId(UUID.randomUUID())
                .fromStatus("CREATED")
                .toStatus("VALIDATED")
                .avgDurationMs(1000L)
                .sampleCount(10)
                .build();

        metric.setSampleCount(25);
        metric.setAvgDurationMs(2000L);
        metric.setHappyPathPct(new BigDecimal("90.00"));

        assertThat(metric.getSampleCount()).isEqualTo(25);
        assertThat(metric.getAvgDurationMs()).isEqualTo(2000L);
        assertThat(metric.getHappyPathPct()).isEqualByComparingTo(new BigDecimal("90.00"));
    }

    @Test
    @DisplayName("metric with zero sample count is valid")
    void builder_ZeroSampleCount_IsValid() {
        ProcessMiningMetric metric = ProcessMiningMetric.builder()
                .insurerId(UUID.randomUUID())
                .fromStatus("REJECTED")
                .toStatus("RETRY_PENDING")
                .avgDurationMs(0L)
                .p95DurationMs(0L)
                .p99DurationMs(0L)
                .sampleCount(0)
                .happyPathPct(BigDecimal.ZERO)
                .build();

        assertThat(metric.getSampleCount()).isZero();
        assertThat(metric.getAvgDurationMs()).isZero();
    }
}
