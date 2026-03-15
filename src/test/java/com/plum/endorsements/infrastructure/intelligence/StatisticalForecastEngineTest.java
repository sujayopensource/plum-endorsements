package com.plum.endorsements.infrastructure.intelligence;

import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.model.EndorsementType;
import com.plum.endorsements.domain.port.BalanceForecastPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("StatisticalForecastEngine")
class StatisticalForecastEngineTest {

    private StatisticalForecastEngine engine;
    private UUID employerId;
    private UUID insurerId;

    @BeforeEach
    void setUp() {
        engine = new StatisticalForecastEngine(30);
        employerId = UUID.randomUUID();
        insurerId = UUID.randomUUID();
    }

    private Endorsement buildHistoryEndorsement(int daysAgo, BigDecimal premium) {
        return Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(UUID.randomUUID())
                .insurerId(insurerId)
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.CONFIRMED)
                .coverageStartDate(LocalDate.now().minusDays(daysAgo - 1))
                .premiumAmount(premium)
                .retryCount(0)
                .createdAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS))
                .build();
    }

    @Test
    @DisplayName("generates forecast from endorsement history")
    void generateForecast_WithHistory_GeneratesForecast() {
        List<Endorsement> history = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            history.add(buildHistoryEndorsement(i, new BigDecimal("1000.00")));
        }

        BalanceForecastPort.ForecastResult result = engine.generateForecast(employerId, insurerId, history);

        assertThat(result).isNotNull();
        assertThat(result.forecastedNeed()).isPositive();
        assertThat(result.daysAhead()).isEqualTo(30);
        assertThat(result.dailyBurnRate()).isPositive();
        assertThat(result.narrative()).isNotEmpty();
        assertThat(result.narrative()).contains("ADD endorsements");
    }

    @Test
    @DisplayName("calculates burn rate from premium amounts")
    void generateForecast_CalculatesBurnRate() {
        List<Endorsement> history = new ArrayList<>();
        // 90 endorsements over 90 days, each with 500 premium
        for (int i = 1; i <= 90; i++) {
            history.add(buildHistoryEndorsement(i, new BigDecimal("500.00")));
        }

        BalanceForecastPort.ForecastResult result = engine.generateForecast(employerId, insurerId, history);

        // dailyEndorsements = 90/90 = 1.0, avgPremium = 500, baseDailyBurnRate = 500
        assertThat(result.dailyBurnRate()).isPositive();
        assertThat(result.forecastedNeed()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("handles empty history gracefully")
    void generateForecast_EmptyHistory_ReturnsZeroForecast() {
        List<Endorsement> history = new ArrayList<>();

        BalanceForecastPort.ForecastResult result = engine.generateForecast(employerId, insurerId, history);

        assertThat(result).isNotNull();
        assertThat(result.forecastedNeed()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.dailyBurnRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.daysAhead()).isEqualTo(30);
    }

    @Test
    @DisplayName("filters history to only ADD type for this employer/insurer")
    void generateForecast_FiltersToRelevantEndorsements() {
        List<Endorsement> history = new ArrayList<>();

        // Add relevant endorsements
        for (int i = 1; i <= 10; i++) {
            history.add(buildHistoryEndorsement(i, new BigDecimal("1000.00")));
        }

        // Add endorsements from different employer (should be filtered)
        UUID otherEmployer = UUID.randomUUID();
        for (int i = 1; i <= 10; i++) {
            Endorsement other = buildHistoryEndorsement(i, new BigDecimal("5000.00"));
            other.setEmployerId(otherEmployer);
            history.add(other);
        }

        // Add DELETE endorsements (should be filtered)
        for (int i = 1; i <= 5; i++) {
            Endorsement del = buildHistoryEndorsement(i, new BigDecimal("1000.00"));
            del.setType(EndorsementType.DELETE);
            history.add(del);
        }

        BalanceForecastPort.ForecastResult result = engine.generateForecast(employerId, insurerId, history);

        // Should only consider the 10 ADD endorsements for this employer/insurer
        assertThat(result.narrative()).contains("10 ADD endorsements");
    }

    @Test
    @DisplayName("narrative includes confidence based on sample size")
    void generateForecast_NarrativeIncludesConfidence() {
        List<Endorsement> history = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            history.add(buildHistoryEndorsement(i, new BigDecimal("800.00")));
        }

        BalanceForecastPort.ForecastResult result = engine.generateForecast(employerId, insurerId, history);

        assertThat(result.narrative()).contains("Confidence:");
        assertThat(result.narrative()).contains("Seasonality-adjusted");
    }

    // --- Phase 3 Edge Case Tests ---

    @Test
    @DisplayName("applies seasonality factor based on forecast date month")
    void shouldApplySeasonalityFactor() {
        // Create a dataset with uniform endorsements
        List<Endorsement> history = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            history.add(buildHistoryEndorsement(i, new BigDecimal("1000.00")));
        }

        BalanceForecastPort.ForecastResult result = engine.generateForecast(employerId, insurerId, history);

        // The narrative should mention seasonality adjustment
        assertThat(result.narrative()).contains("Seasonality-adjusted");
        // Forecast should be positive (not just raw daily * 30 due to seasonality)
        assertThat(result.forecastedNeed()).isPositive();
        assertThat(result.dailyBurnRate()).isPositive();
    }

    @Test
    @DisplayName("handles single data point as degenerate case")
    void shouldHandleSingleDataPoint() {
        List<Endorsement> history = new ArrayList<>();
        // Only 1 endorsement in 90-day window
        history.add(buildHistoryEndorsement(1, new BigDecimal("5000.00")));

        BalanceForecastPort.ForecastResult result = engine.generateForecast(employerId, insurerId, history);

        assertThat(result).isNotNull();
        assertThat(result.daysAhead()).isEqualTo(30);
        // With 1 endorsement in 90 days, daily rate = 5000 * (1/90) ~ 55.56/day
        assertThat(result.dailyBurnRate()).isPositive();
        assertThat(result.forecastedNeed()).isPositive();
        // Confidence should be low given limited data
        assertThat(result.narrative()).contains("1 ADD endorsements");
    }

    @Test
    @DisplayName("factors in delete credit delay — deletions reduce forecast only after delay period")
    void generateForecast_FactorsInCreditDelay() {
        // Use a short credit delay so credits kick in within the 30-day forecast window
        StatisticalForecastEngine shortDelayEngine = new StatisticalForecastEngine(5);
        StatisticalForecastEngine longDelayEngine = new StatisticalForecastEngine(29);

        List<Endorsement> history = new ArrayList<>();
        // Add ADDs
        for (int i = 1; i <= 30; i++) {
            history.add(buildHistoryEndorsement(i, new BigDecimal("1000.00")));
        }
        // Add DELETEs (same employer/insurer)
        for (int i = 1; i <= 15; i++) {
            Endorsement del = buildHistoryEndorsement(i, new BigDecimal("800.00"));
            del.setType(EndorsementType.DELETE);
            history.add(del);
        }

        BalanceForecastPort.ForecastResult shortDelayResult =
                shortDelayEngine.generateForecast(employerId, insurerId, history);
        BalanceForecastPort.ForecastResult longDelayResult =
                longDelayEngine.generateForecast(employerId, insurerId, history);

        // Short delay = more credits applied within window = lower forecasted need
        assertThat(shortDelayResult.forecastedNeed())
                .isLessThan(longDelayResult.forecastedNeed());

        // Both should mention credit delay in narrative
        assertThat(shortDelayResult.narrative()).contains("Delete credits");
        assertThat(longDelayResult.narrative()).contains("Delete credits");
    }

    @Test
    @DisplayName("projects positive forecast for high burn rate employer")
    void shouldProjectPositiveForHighBurnRate() {
        // Simulate a large employer with many endorsements daily
        List<Endorsement> history = new ArrayList<>();
        for (int i = 1; i <= 89; i++) {
            // 5 endorsements per day, each with 2000 premium
            for (int j = 0; j < 5; j++) {
                history.add(buildHistoryEndorsement(i, new BigDecimal("2000.00")));
            }
        }

        BalanceForecastPort.ForecastResult result = engine.generateForecast(employerId, insurerId, history);

        assertThat(result).isNotNull();
        // High daily burn rate with ~5 endorsements/day at 2000 premium each
        assertThat(result.dailyBurnRate()).isGreaterThan(BigDecimal.ZERO);
        // Total forecast for 30 days should be substantial
        assertThat(result.forecastedNeed()).isGreaterThan(new BigDecimal("100000.00"));
        assertThat(result.narrative()).contains("ADD endorsements");
        assertThat(result.narrative()).contains("avg premium");
    }
}
