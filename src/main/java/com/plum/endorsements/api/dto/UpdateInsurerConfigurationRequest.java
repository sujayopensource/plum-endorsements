package com.plum.endorsements.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateInsurerConfigurationRequest(
        @Size(max = 100) String insurerName,
        Boolean supportsRealTime,
        Boolean supportsBatch,
        Integer maxBatchSize,
        Long batchSlaHours,
        Integer rateLimitPerMinute,
        String apiBaseUrl,
        String authType,
        String dataFormat,
        Boolean active
) {}
