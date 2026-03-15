package com.plum.endorsements.api.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record StpRateResponse(
        BigDecimal overallStpRate,
        Map<UUID, BigDecimal> perInsurerStpRate,
        long totalProcessed,
        long successfulCount
) {
    public BigDecimal stpRate() {
        return overallStpRate;
    }
}
