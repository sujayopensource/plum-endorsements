package com.plum.endorsements.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceForecastRecord {

    private UUID id;
    private UUID employerId;
    private UUID insurerId;
    private LocalDate forecastDate;
    private BigDecimal forecastedAmount;
    private BigDecimal actualAmount;
    private BigDecimal accuracy;
    private String narrative;
    private Instant createdAt;

    public BigDecimal calculateAccuracy() {
        if (actualAmount == null || forecastedAmount == null || forecastedAmount.signum() == 0) {
            return null;
        }
        BigDecimal error = actualAmount.subtract(forecastedAmount).abs();
        BigDecimal errorRate = error.divide(forecastedAmount, 4, RoundingMode.HALF_UP);
        return BigDecimal.ONE.subtract(errorRate).max(BigDecimal.ZERO);
    }

    public void recordActual(BigDecimal actual) {
        this.actualAmount = actual;
        this.accuracy = calculateAccuracy();
    }
}
