package com.plum.endorsements.domain.model;

import com.plum.endorsements.domain.port.InsurerPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InsurerConfiguration")
class InsurerConfigurationTest {

    @Test
    @DisplayName("toCapabilities maps all fields correctly")
    void toCapabilities_MapsAllFields() {
        InsurerConfiguration config = InsurerConfiguration.builder()
                .insurerId(UUID.randomUUID())
                .insurerName("Test Insurer")
                .insurerCode("TEST")
                .adapterType("MOCK")
                .supportsRealTime(true)
                .supportsBatch(true)
                .maxBatchSize(200)
                .batchSlaHours(48)
                .rateLimitPerMinute(120)
                .dataFormat("JSON")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        InsurerPort.InsurerCapabilities capabilities = config.toCapabilities();

        assertThat(capabilities.supportsRealTime()).isTrue();
        assertThat(capabilities.supportsBatch()).isTrue();
        assertThat(capabilities.maxBatchSize()).isEqualTo(200);
        assertThat(capabilities.batchSlaHours()).isEqualTo(48);
        assertThat(capabilities.rateLimitPerMinute()).isEqualTo(120);
    }

    @Test
    @DisplayName("toCapabilities for real-time only insurer")
    void toCapabilities_RealTimeOnly() {
        InsurerConfiguration config = InsurerConfiguration.builder()
                .insurerId(UUID.randomUUID())
                .insurerName("ICICI Lombard")
                .insurerCode("ICICI_LOMBARD")
                .adapterType("ICICI_LOMBARD")
                .supportsRealTime(true)
                .supportsBatch(false)
                .maxBatchSize(0)
                .batchSlaHours(0)
                .rateLimitPerMinute(120)
                .dataFormat("JSON")
                .active(true)
                .build();

        InsurerPort.InsurerCapabilities capabilities = config.toCapabilities();

        assertThat(capabilities.supportsRealTime()).isTrue();
        assertThat(capabilities.supportsBatch()).isFalse();
        assertThat(capabilities.maxBatchSize()).isZero();
    }

    @Test
    @DisplayName("toCapabilities for batch-only insurer")
    void toCapabilities_BatchOnly() {
        InsurerConfiguration config = InsurerConfiguration.builder()
                .insurerId(UUID.randomUUID())
                .insurerName("Niva Bupa")
                .insurerCode("NIVA_BUPA")
                .adapterType("NIVA_BUPA")
                .supportsRealTime(false)
                .supportsBatch(true)
                .maxBatchSize(500)
                .batchSlaHours(24)
                .rateLimitPerMinute(0)
                .dataFormat("CSV")
                .active(true)
                .build();

        InsurerPort.InsurerCapabilities capabilities = config.toCapabilities();

        assertThat(capabilities.supportsRealTime()).isFalse();
        assertThat(capabilities.supportsBatch()).isTrue();
        assertThat(capabilities.maxBatchSize()).isEqualTo(500);
        assertThat(capabilities.batchSlaHours()).isEqualTo(24);
    }

    @Test
    @DisplayName("builder defaults are sensible")
    void builder_DefaultValues() {
        InsurerConfiguration config = InsurerConfiguration.builder()
                .insurerId(UUID.randomUUID())
                .insurerName("Default")
                .insurerCode("DEFAULT")
                .adapterType("MOCK")
                .build();

        assertThat(config.isActive()).isFalse();
        assertThat(config.getAuthConfig()).isNull();
        assertThat(config.getCircuitBreakerConfig()).isNull();
        assertThat(config.getApiBaseUrl()).isNull();
    }

    @Test
    @DisplayName("all fields accessible via getters")
    void allFieldsAccessible() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        InsurerConfiguration config = InsurerConfiguration.builder()
                .insurerId(id)
                .insurerName("Bajaj Allianz")
                .insurerCode("BAJAJ_ALLIANZ")
                .adapterType("BAJAJ_ALLIANZ")
                .supportsRealTime(true)
                .supportsBatch(true)
                .maxBatchSize(200)
                .batchSlaHours(4)
                .rateLimitPerMinute(30)
                .apiBaseUrl("https://api.bajajallianz.com/ws")
                .authType("WS_SECURITY")
                .dataFormat("XML")
                .retryMaxAttempts(5)
                .retryWaitMs(3000)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(config.getInsurerId()).isEqualTo(id);
        assertThat(config.getInsurerName()).isEqualTo("Bajaj Allianz");
        assertThat(config.getInsurerCode()).isEqualTo("BAJAJ_ALLIANZ");
        assertThat(config.getAdapterType()).isEqualTo("BAJAJ_ALLIANZ");
        assertThat(config.getApiBaseUrl()).isEqualTo("https://api.bajajallianz.com/ws");
        assertThat(config.getAuthType()).isEqualTo("WS_SECURITY");
        assertThat(config.getDataFormat()).isEqualTo("XML");
        assertThat(config.getRetryMaxAttempts()).isEqualTo(5);
        assertThat(config.getRetryWaitMs()).isEqualTo(3000);
        assertThat(config.getCreatedAt()).isEqualTo(now);
        assertThat(config.getUpdatedAt()).isEqualTo(now);
    }
}
