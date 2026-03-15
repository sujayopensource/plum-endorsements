package com.plum.endorsements.infrastructure.intelligence;

import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementType;
import com.plum.endorsements.domain.port.BalanceForecastPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class StatisticalForecastEngine implements BalanceForecastPort {

    private static final int HISTORY_DAYS = 90;
    private static final Map<DayOfWeek, Double> DAY_OF_WEEK_FACTORS = Map.of(
            DayOfWeek.MONDAY, 1.2,
            DayOfWeek.TUESDAY, 1.15,
            DayOfWeek.WEDNESDAY, 1.1,
            DayOfWeek.THURSDAY, 1.05,
            DayOfWeek.FRIDAY, 1.0,
            DayOfWeek.SATURDAY, 0.3,
            DayOfWeek.SUNDAY, 0.2
    );

    // Indian business seasonality: hiring waves in April/October
    private static final Map<Integer, Double> MONTH_FACTORS = Map.ofEntries(
            Map.entry(1, 0.9),   // January
            Map.entry(2, 0.95),  // February
            Map.entry(3, 1.1),   // March (fiscal year end)
            Map.entry(4, 1.4),   // April (new fiscal year, hiring wave)
            Map.entry(5, 1.1),   // May
            Map.entry(6, 1.0),   // June
            Map.entry(7, 1.0),   // July
            Map.entry(8, 0.95),  // August
            Map.entry(9, 1.05),  // September
            Map.entry(10, 1.3),  // October (appraisal cycle, hiring wave)
            Map.entry(11, 1.05), // November
            Map.entry(12, 0.85)  // December
    );

    private final int creditDelayDays;

    public StatisticalForecastEngine(
            @Value("${endorsement.ea.credit-delay-days:30}") int creditDelayDays) {
        this.creditDelayDays = creditDelayDays;
    }

    @Override
    public ForecastResult generateForecast(UUID employerId, UUID insurerId, List<Endorsement> history) {
        Instant cutoff = Instant.now().minus(HISTORY_DAYS, ChronoUnit.DAYS);

        List<Endorsement> relevantForEmployerInsurer = history.stream()
                .filter(e -> e.getEmployerId().equals(employerId))
                .filter(e -> e.getInsurerId().equals(insurerId))
                .filter(e -> e.getCreatedAt() != null && e.getCreatedAt().isAfter(cutoff))
                .toList();

        // Filter to ADD endorsements for burn rate
        List<Endorsement> relevantAdds = relevantForEmployerInsurer.stream()
                .filter(e -> e.getType() == EndorsementType.ADD)
                .toList();

        // Filter to DELETE endorsements for credit rate
        List<Endorsement> relevantDeletes = relevantForEmployerInsurer.stream()
                .filter(e -> e.getType() == EndorsementType.DELETE)
                .toList();

        // Calculate daily burn rate from ADDs
        DescriptiveStatistics premiumStats = new DescriptiveStatistics();
        relevantAdds.stream()
                .filter(e -> e.getPremiumAmount() != null)
                .forEach(e -> premiumStats.addValue(e.getPremiumAmount().doubleValue()));

        double avgPremium = premiumStats.getN() > 0 ? premiumStats.getMean() : 0;
        double dailyEndorsements = relevantAdds.size() / (double) HISTORY_DAYS;
        double baseDailyBurnRate = avgPremium * dailyEndorsements;

        // Calculate daily credit rate from DELETEs (delayed by creditDelayDays)
        DescriptiveStatistics creditStats = new DescriptiveStatistics();
        relevantDeletes.stream()
                .filter(e -> e.getPremiumAmount() != null)
                .forEach(e -> creditStats.addValue(e.getPremiumAmount().doubleValue()));

        double avgCreditPremium = creditStats.getN() > 0 ? creditStats.getMean() : 0;
        double dailyDeleteRate = relevantDeletes.size() / (double) HISTORY_DAYS;
        double baseDailyCreditRate = avgCreditPremium * dailyDeleteRate;

        // Project 30 days ahead with seasonality
        int forecastDays = 30;
        double totalForecastedNeed = 0;
        double totalDelayedCredits = 0;
        LocalDate today = LocalDate.now();

        for (int day = 1; day <= forecastDays; day++) {
            LocalDate forecastDate = today.plusDays(day);
            double dayFactor = DAY_OF_WEEK_FACTORS.getOrDefault(forecastDate.getDayOfWeek(), 1.0);
            double monthFactor = MONTH_FACTORS.getOrDefault(forecastDate.getMonthValue(), 1.0);
            totalForecastedNeed += baseDailyBurnRate * dayFactor * monthFactor;

            // Credits from deletions are delayed: only count credits for deletions
            // that would have been processed creditDelayDays ago
            if (day > creditDelayDays) {
                totalDelayedCredits += baseDailyCreditRate * dayFactor * monthFactor;
            }
        }

        // Net need = gross burn - delayed credits
        double netForecastedNeed = Math.max(0, totalForecastedNeed - totalDelayedCredits);

        BigDecimal forecastedNeed = BigDecimal.valueOf(netForecastedNeed).setScale(2, RoundingMode.HALF_UP);
        BigDecimal dailyBurnRate = BigDecimal.valueOf(baseDailyBurnRate).setScale(2, RoundingMode.HALF_UP);

        // Calculate shortfall (caller computes against current balance)
        BigDecimal shortfall = BigDecimal.ZERO;
        boolean topUpRequired = false;

        // Generate narrative
        double confidence = Math.min(95, 50 + (premiumStats.getN() * 0.5));
        String creditNarrative = baseDailyCreditRate > 0
                ? String.format(" Delete credits (₹%.2f/day, %d-day delay) offset ₹%.2f.",
                        baseDailyCreditRate, creditDelayDays,
                        BigDecimal.valueOf(totalDelayedCredits).setScale(2, RoundingMode.HALF_UP))
                : "";
        String narrative = String.format(
                "Based on %d-day trends (%d ADD endorsements, avg premium ₹%.2f), " +
                "employer will need approximately ₹%s over the next %d days. " +
                "Daily burn rate: ₹%s.%s Seasonality-adjusted. Confidence: %.0f%%.",
                HISTORY_DAYS, relevantAdds.size(), avgPremium,
                forecastedNeed.toPlainString(), forecastDays,
                dailyBurnRate.toPlainString(), creditNarrative, confidence);

        log.debug("Forecast for employer {} insurer {}: need={}, burnRate={}/day, creditRate={}/day, creditDelay={}d",
                employerId, insurerId, forecastedNeed, dailyBurnRate, baseDailyCreditRate, creditDelayDays);

        return new ForecastResult(forecastedNeed, forecastDays, dailyBurnRate,
                shortfall, topUpRequired, narrative);
    }
}
