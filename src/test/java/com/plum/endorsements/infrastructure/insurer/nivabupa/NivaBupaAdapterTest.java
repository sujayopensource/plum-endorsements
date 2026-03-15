package com.plum.endorsements.infrastructure.insurer.nivabupa;

import com.plum.endorsements.domain.port.InsurerPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NivaBupaAdapter")
class NivaBupaAdapterTest {

    private NivaBupaAdapter adapter;

    @BeforeEach
    void setUp() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        NivaBupaCsvMapper csvMapper = new NivaBupaCsvMapper();
        adapter = new NivaBupaAdapter(meterRegistry, csvMapper);
    }

    @Test
    @DisplayName("submitRealTime throws UnsupportedOperationException")
    void submitRealTime_ThrowsUnsupported() {
        assertThatThrownBy(() -> adapter.submitRealTime(UUID.randomUUID(), Map.of()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("does not support real-time");
    }

    @Test
    @DisplayName("submitBatch returns successful reference")
    void submitBatch_ReturnsReference() {
        UUID batchId = UUID.randomUUID();
        List<Map<String, Object>> endorsements = List.of(
                Map.of("policy_id", "POL-1", "employee_id", "EMP-1", "employee_name", "User 1",
                        "type", "ADDITION", "coverage_start_date", "2026-04-01",
                        "premium_amount", 50000, "date_of_birth", "1990-01-01",
                        "gender", "M", "relationship", "SELF")
        );

        String reference = adapter.submitBatch(batchId, endorsements);

        assertThat(reference).startsWith("NIVA-BATCH-");
    }

    @Test
    @DisplayName("checkBatchStatus returns COMPLETED status")
    void checkBatchStatus_ReturnsCompleted() {
        InsurerPort.BatchStatusResult result = adapter.checkBatchStatus("NIVA-BATCH-12345678");

        assertThat(result.status()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("capabilities report batch only")
    void getCapabilities_BatchOnly() {
        InsurerPort.InsurerCapabilities capabilities = adapter.getCapabilities();

        assertThat(capabilities.supportsRealTime()).isFalse();
        assertThat(capabilities.supportsBatch()).isTrue();
        assertThat(capabilities.maxBatchSize()).isEqualTo(500);
        assertThat(capabilities.batchSlaHours()).isEqualTo(24);
        assertThat(capabilities.rateLimitPerMinute()).isZero();
    }

    @Test
    @DisplayName("adapter type is NIVA_BUPA")
    void getAdapterType_ReturnsCorrectType() {
        assertThat(adapter.getAdapterType()).isEqualTo("NIVA_BUPA");
    }
}
