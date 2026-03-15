package com.plum.endorsements.api.dto;

import com.plum.endorsements.domain.model.BalanceForecastRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BalanceForecastResponse(
        UUID id,
        UUID employerId,
        UUID insurerId,
        LocalDate forecastDate,
        BigDecimal forecastedAmount,
        BigDecimal actualAmount,
        BigDecimal accuracy,
        String narrative,
        Instant createdAt
) {
    public static BalanceForecastResponse from(BalanceForecastRecord record) {
        return new BalanceForecastResponse(
                record.getId(),
                record.getEmployerId(),
                record.getInsurerId(),
                record.getForecastDate(),
                record.getForecastedAmount(),
                record.getActualAmount(),
                record.getAccuracy(),
                record.getNarrative(),
                record.getCreatedAt()
        );
    }
}
