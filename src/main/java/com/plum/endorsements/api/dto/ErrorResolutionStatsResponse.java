package com.plum.endorsements.api.dto;

public record ErrorResolutionStatsResponse(
        long totalResolutions,
        long autoApplied,
        long suggested,
        double autoApplyRate,
        long successCount,
        long failureCount,
        double successRate
) {}
