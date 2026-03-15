package com.plum.endorsements.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StpRateTrendResponse(
    UUID insurerId,
    List<DataPoint> dataPoints,
    BigDecimal currentRate,
    BigDecimal changePercent
) {
    public record DataPoint(LocalDate date, BigDecimal stpRate, int total, int stp) {}
}
