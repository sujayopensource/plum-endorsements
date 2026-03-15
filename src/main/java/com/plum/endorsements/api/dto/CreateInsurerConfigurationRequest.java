package com.plum.endorsements.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateInsurerConfigurationRequest(
        @NotBlank @Size(max = 100) String insurerName,
        @NotBlank @Size(max = 20) String insurerCode,
        @NotBlank @Size(max = 30) String adapterType,
        @NotNull Boolean supportsRealTime,
        @NotNull Boolean supportsBatch,
        @Positive int maxBatchSize,
        @Positive long batchSlaHours,
        @Positive int rateLimitPerMinute,
        String apiBaseUrl,
        String authType,
        String dataFormat
) {}
