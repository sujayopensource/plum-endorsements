package com.plum.endorsements.infrastructure.insurer.bajaj;

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

@DisplayName("BajajAllianzAdapter")
class BajajAllianzAdapterTest {

    private BajajAllianzAdapter adapter;

    @BeforeEach
    void setUp() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        BajajAllianzXmlMapper xmlMapper = new BajajAllianzXmlMapper();
        adapter = new BajajAllianzAdapter(meterRegistry, xmlMapper);
    }

    @Test
    @DisplayName("submitRealTime returns successful result with BAJAJ reference")
    void submitRealTime_ReturnsSuccess() {
        UUID endorsementId = UUID.randomUUID();
        Map<String, Object> data = Map.of("employee_name", "Test", "type", "ADDITION");

        InsurerPort.SubmissionResult result = adapter.submitRealTime(endorsementId, data);

        assertThat(result.success()).isTrue();
        assertThat(result.insurerReference()).startsWith("BAJAJ-");
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    @DisplayName("submitBatch returns batch reference")
    void submitBatch_ReturnsBatchReference() {
        UUID batchId = UUID.randomUUID();
        List<Map<String, Object>> endorsements = List.of(
                Map.of("policy_id", "POL-1", "employee_name", "User 1", "type", "ADDITION")
        );

        String reference = adapter.submitBatch(batchId, endorsements);

        assertThat(reference).startsWith("BAJAJ-BATCH-");
    }

    @Test
    @DisplayName("checkBatchStatus returns completed status")
    void checkBatchStatus_ReturnsCompleted() {
        InsurerPort.BatchStatusResult result = adapter.checkBatchStatus("BAJAJ-BATCH-12345678");

        assertThat(result.status()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("capabilities report both real-time and batch")
    void getCapabilities_BothModes() {
        InsurerPort.InsurerCapabilities capabilities = adapter.getCapabilities();

        assertThat(capabilities.supportsRealTime()).isTrue();
        assertThat(capabilities.supportsBatch()).isTrue();
        assertThat(capabilities.maxBatchSize()).isEqualTo(200);
        assertThat(capabilities.batchSlaHours()).isEqualTo(4);
        assertThat(capabilities.rateLimitPerMinute()).isEqualTo(30);
    }

    @Test
    @DisplayName("adapter type is BAJAJ_ALLIANZ")
    void getAdapterType_ReturnsCorrectType() {
        assertThat(adapter.getAdapterType()).isEqualTo("BAJAJ_ALLIANZ");
    }

    @Test
    @DisplayName("mapToInsurerFormat delegates to XML mapper")
    void mapToInsurerFormat_DelegatesToMapper() {
        Map<String, Object> input = Map.of(
                "employee_name", "Test",
                "type", "DELETION",
                "policy_id", "POL-1"
        );

        Map<String, Object> result = adapter.mapToInsurerFormat(input);

        assertThat(result.get("MemberName")).isEqualTo("Test");
        assertThat(result.get("EndorsementType")).isEqualTo("DELETE_MEMBER");
    }
}
