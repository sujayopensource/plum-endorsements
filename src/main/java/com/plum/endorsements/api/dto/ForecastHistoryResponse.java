package com.plum.endorsements.api.dto;

import com.plum.endorsements.domain.model.BalanceForecastRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ForecastHistoryResponse(
        UUID id,
        UUID employerId,
        UUID insurerId,
        LocalDate forecastDate,
        BigDecimal forecastedAmount,
        BigDecimal actualAmount,
        BigDecimal accuracy,
        Instant createdAt
) {
    public static ForecastHistoryResponse from(BalanceForecastRecord record) {
        return new ForecastHistoryResponse(
                record.getId(),
                record.getEmployerId(),
                record.getInsurerId(),
                record.getForecastDate(),
                record.getForecastedAmount(),
                record.getActualAmount(),
                record.getAccuracy(),
                record.getCreatedAt()
        );
    }
}
