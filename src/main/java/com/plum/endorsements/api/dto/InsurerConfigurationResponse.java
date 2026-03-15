package com.plum.endorsements.api.dto;

import com.plum.endorsements.domain.model.InsurerConfiguration;
import com.plum.endorsements.domain.port.InsurerPort;

import java.time.Instant;
import java.util.UUID;

public record InsurerConfigurationResponse(
        UUID insurerId,
        String insurerName,
        String insurerCode,
        String adapterType,
        boolean supportsRealTime,
        boolean supportsBatch,
        int maxBatchSize,
        long batchSlaHours,
        int rateLimitPerMinute,
        String dataFormat,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static InsurerConfigurationResponse from(InsurerConfiguration config) {
        return new InsurerConfigurationResponse(
                config.getInsurerId(),
                config.getInsurerName(),
                config.getInsurerCode(),
                config.getAdapterType(),
                config.isSupportsRealTime(),
                config.isSupportsBatch(),
                config.getMaxBatchSize(),
                config.getBatchSlaHours(),
                config.getRateLimitPerMinute(),
                config.getDataFormat(),
                config.isActive(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }

    public record CapabilitiesResponse(
            boolean supportsRealTime,
            boolean supportsBatch,
            int maxBatchSize,
            long batchSlaHours,
            int rateLimitPerMinute
    ) {
        public static CapabilitiesResponse from(InsurerPort.InsurerCapabilities caps) {
            return new CapabilitiesResponse(
                    caps.supportsRealTime(), caps.supportsBatch(),
                    caps.maxBatchSize(), caps.batchSlaHours(), caps.rateLimitPerMinute()
            );
        }
    }
}
