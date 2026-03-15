package com.plum.endorsements.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BalanceForecastRecord domain model")
class BalanceForecastRecordTest {

    private BalanceForecastRecord buildForecast(BigDecimal forecasted) {
        return BalanceForecastRecord.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .forecastDate(LocalDate.now().plusDays(30))
                .forecastedAmount(forecasted)
                .narrative("Based on 90-day trends")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("calculateAccuracy returns correct value for close prediction")
    void calculateAccuracy_ClosePrediction_ReturnsHighAccuracy() {
        BalanceForecastRecord record = buildForecast(new BigDecimal("10000.00"));
        record.setActualAmount(new BigDecimal("9800.00"));

        BigDecimal accuracy = record.calculateAccuracy();

        // error = |9800 - 10000| = 200, errorRate = 200/10000 = 0.02, accuracy = 1 - 0.02 = 0.98
        assertThat(accuracy).isEqualByComparingTo(new BigDecimal("0.9800"));
    }

    @Test
    @DisplayName("calculateAccuracy returns zero when actual far exceeds forecast")
    void calculateAccuracy_ActualFarExceedsForecast_ReturnsZero() {
        BalanceForecastRecord record = buildForecast(new BigDecimal("1000.00"));
        record.setActualAmount(new BigDecimal("3000.00"));

        BigDecimal accuracy = record.calculateAccuracy();

        // error = |3000 - 1000| = 2000, errorRate = 2000/1000 = 2.0, accuracy = max(0, 1 - 2.0) = 0
        assertThat(accuracy).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("calculateAccuracy returns null when actualAmount is null")
    void calculateAccuracy_NullActual_ReturnsNull() {
        BalanceForecastRecord record = buildForecast(new BigDecimal("10000.00"));
        // actualAmount is not set — remains null

        BigDecimal accuracy = record.calculateAccuracy();

        assertThat(accuracy).isNull();
    }

    @Test
    @DisplayName("calculateAccuracy returns null when forecastedAmount is zero")
    void calculateAccuracy_ZeroForecast_ReturnsNull() {
        BalanceForecastRecord record = buildForecast(BigDecimal.ZERO);
        record.setActualAmount(new BigDecimal("500.00"));

        BigDecimal accuracy = record.calculateAccuracy();

        assertThat(accuracy).isNull();
    }

    @Test
    @DisplayName("recordActual sets actualAmount and calculates accuracy")
    void recordActual_SetsActualAndAccuracy() {
        BalanceForecastRecord record = buildForecast(new BigDecimal("5000.00"));

        record.recordActual(new BigDecimal("4500.00"));

        assertThat(record.getActualAmount()).isEqualByComparingTo(new BigDecimal("4500.00"));
        assertThat(record.getAccuracy()).isNotNull();
        // error = |4500 - 5000| = 500, errorRate = 500/5000 = 0.1, accuracy = 1 - 0.1 = 0.9
        assertThat(record.getAccuracy()).isEqualByComparingTo(new BigDecimal("0.9000"));
    }

    @Test
    @DisplayName("calculateAccuracy returns null when forecastedAmount is null")
    void calculateAccuracy_NullForecast_ReturnsNull() {
        BalanceForecastRecord record = BalanceForecastRecord.builder()
                .id(UUID.randomUUID())
                .forecastedAmount(null)
                .actualAmount(new BigDecimal("1000.00"))
                .build();

        BigDecimal accuracy = record.calculateAccuracy();

        assertThat(accuracy).isNull();
    }
}
