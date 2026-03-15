package com.plum.endorsements.infrastructure.insurer.icici;

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

@DisplayName("IciciLombardAdapter")
class IciciLombardAdapterTest {

    private IciciLombardAdapter adapter;

    @BeforeEach
    void setUp() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        IciciLombardDataMapper dataMapper = new IciciLombardDataMapper();
        adapter = new IciciLombardAdapter(meterRegistry, dataMapper);
    }

    @Test
    @DisplayName("submitRealTime returns successful result with ICICI reference")
    void submitRealTime_ReturnsSuccess() {
        UUID endorsementId = UUID.randomUUID();
        Map<String, Object> data = Map.of("employee_name", "Test", "type", "ADDITION");

        InsurerPort.SubmissionResult result = adapter.submitRealTime(endorsementId, data);

        assertThat(result.success()).isTrue();
        assertThat(result.insurerReference()).startsWith("ICICI-");
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    @DisplayName("submitBatch throws UnsupportedOperationException")
    void submitBatch_ThrowsUnsupported() {
        assertThatThrownBy(() -> adapter.submitBatch(UUID.randomUUID(), List.of()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("does not support batch");
    }

    @Test
    @DisplayName("checkBatchStatus throws UnsupportedOperationException")
    void checkBatchStatus_ThrowsUnsupported() {
        assertThatThrownBy(() -> adapter.checkBatchStatus("REF-123"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("capabilities report real-time only")
    void getCapabilities_RealTimeOnly() {
        InsurerPort.InsurerCapabilities capabilities = adapter.getCapabilities();

        assertThat(capabilities.supportsRealTime()).isTrue();
        assertThat(capabilities.supportsBatch()).isFalse();
        assertThat(capabilities.maxBatchSize()).isZero();
        assertThat(capabilities.rateLimitPerMinute()).isEqualTo(120);
    }

    @Test
    @DisplayName("adapter type is ICICI_LOMBARD")
    void getAdapterType_ReturnsCorrectType() {
        assertThat(adapter.getAdapterType()).isEqualTo("ICICI_LOMBARD");
    }

    @Test
    @DisplayName("mapToInsurerFormat delegates to data mapper")
    void mapToInsurerFormat_DelegatesToMapper() {
        Map<String, Object> input = Map.of("employee_name", "Test", "type", "ADDITION");

        Map<String, Object> result = adapter.mapToInsurerFormat(input);

        assertThat(result.get("memberName")).isEqualTo("Test");
        assertThat(result.get("endorsementType")).isEqualTo("ADD");
    }
}
