package com.plum.endorsements.infrastructure.intelligence;

import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class RuleBasedAnomalyScorer {

    public ScoringResult score(Endorsement endorsement, List<Endorsement> recentHistory) {
        List<ScoringResult> results = new ArrayList<>();

        results.add(checkVolumeSpike(endorsement, recentHistory));
        results.add(checkAddDeleteCycling(endorsement, recentHistory));
        results.add(checkSuspiciousTiming(endorsement, recentHistory));
        results.add(checkUnusualPremium(endorsement, recentHistory));
        results.add(checkDormancyBreak(endorsement, recentHistory));

        return results.stream()
                .max((a, b) -> Double.compare(a.score(), b.score()))
                .orElse(new ScoringResult("NONE", 0.0, "No anomaly detected"));
    }

    private ScoringResult checkVolumeSpike(Endorsement endorsement, List<Endorsement> history) {
        UUID employerId = endorsement.getEmployerId();
        Instant last24h = Instant.now().minus(24, ChronoUnit.HOURS);

        long recentCount = history.stream()
                .filter(e -> e.getEmployerId().equals(employerId))
                .filter(e -> e.getCreatedAt() != null && e.getCreatedAt().isAfter(last24h))
                .count();

        Instant last30d = Instant.now().minus(30, ChronoUnit.DAYS);
        long totalInMonth = history.stream()
                .filter(e -> e.getEmployerId().equals(employerId))
                .filter(e -> e.getCreatedAt() != null && e.getCreatedAt().isAfter(last30d))
                .count();
        double dailyAvg = totalInMonth / 30.0;

        if (recentCount >= 10 && dailyAvg > 0 && recentCount > dailyAvg * 5) {
            double score = Math.min(0.95, 0.5 + (recentCount / (dailyAvg * 10)));
            return new ScoringResult("VOLUME_SPIKE", score,
                    String.format("Volume spike detected: %d endorsements in 24h vs average of %.1f/day for employer %s",
                            recentCount, dailyAvg, employerId));
        }

        return new ScoringResult("VOLUME_SPIKE", 0.0, "Normal volume");
    }

    private ScoringResult checkAddDeleteCycling(Endorsement endorsement, List<Endorsement> history) {
        UUID employeeId = endorsement.getEmployeeId();
        UUID employerId = endorsement.getEmployerId();
        Instant windowStart = Instant.now().minus(30, ChronoUnit.DAYS);

        List<Endorsement> employeeHistory = history.stream()
                .filter(e -> e.getEmployeeId() != null && e.getEmployeeId().equals(employeeId))
                .filter(e -> e.getEmployerId().equals(employerId))
                .filter(e -> e.getCreatedAt() != null && e.getCreatedAt().isAfter(windowStart))
                .toList();

        boolean hasAdd = employeeHistory.stream().anyMatch(e -> e.getType() == EndorsementType.ADD);
        boolean hasDelete = employeeHistory.stream().anyMatch(e -> e.getType() == EndorsementType.DELETE);

        if (hasAdd && hasDelete) {
            return new ScoringResult("ADD_DELETE_CYCLING", 0.85,
                    String.format("Add/delete cycling detected: employee %s has both ADD and DELETE endorsements within 30 days for employer %s",
                            employeeId, employerId));
        }

        return new ScoringResult("ADD_DELETE_CYCLING", 0.0, "No cycling detected");
    }

    private ScoringResult checkSuspiciousTiming(Endorsement endorsement, List<Endorsement> history) {
        if (endorsement.getType() != EndorsementType.ADD) {
            return new ScoringResult("SUSPICIOUS_TIMING", 0.0, "Not an addition");
        }

        if (endorsement.getCoverageStartDate() != null) {
            long daysUntilCoverage = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(), endorsement.getCoverageStartDate());
            if (daysUntilCoverage <= 7 && daysUntilCoverage >= 0) {
                return new ScoringResult("SUSPICIOUS_TIMING", 0.75,
                        String.format("Suspicious timing: ADD endorsement with coverage starting in %d days (possible pre-claim addition)",
                                daysUntilCoverage));
            }
        }

        return new ScoringResult("SUSPICIOUS_TIMING", 0.0, "Normal timing");
    }

    private ScoringResult checkUnusualPremium(Endorsement endorsement, List<Endorsement> history) {
        if (endorsement.getPremiumAmount() == null) {
            return new ScoringResult("UNUSUAL_PREMIUM", 0.0, "No premium to check");
        }

        UUID employerId = endorsement.getEmployerId();
        DescriptiveStatistics stats = new DescriptiveStatistics();

        history.stream()
                .filter(e -> e.getEmployerId().equals(employerId))
                .filter(e -> e.getPremiumAmount() != null)
                .forEach(e -> stats.addValue(e.getPremiumAmount().doubleValue()));

        if (stats.getN() < 5) {
            return new ScoringResult("UNUSUAL_PREMIUM", 0.0, "Insufficient data for premium analysis");
        }

        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();
        double premium = endorsement.getPremiumAmount().doubleValue();

        if (stdDev > 0 && Math.abs(premium - mean) > 3 * stdDev) {
            return new ScoringResult("UNUSUAL_PREMIUM", 0.7,
                    String.format("Unusual premium: ₹%.2f is %.1f standard deviations from employer average of ₹%.2f",
                            premium, Math.abs(premium - mean) / stdDev, mean));
        }

        return new ScoringResult("UNUSUAL_PREMIUM", 0.0, "Premium within normal range");
    }

    private ScoringResult checkDormancyBreak(Endorsement endorsement, List<Endorsement> history) {
        UUID employeeId = endorsement.getEmployeeId();
        if (employeeId == null) {
            return new ScoringResult("DORMANCY_BREAK", 0.0, "No employee ID to check");
        }

        Instant mostRecent = history.stream()
                .filter(e -> !e.getId().equals(endorsement.getId()))
                .filter(e -> e.getEmployeeId() != null && e.getEmployeeId().equals(employeeId))
                .filter(e -> e.getCreatedAt() != null)
                .map(Endorsement::getCreatedAt)
                .max(Instant::compareTo)
                .orElse(null);

        if (mostRecent == null) {
            return new ScoringResult("DORMANCY_BREAK", 0.0, "No history for employee");
        }

        long daysSinceLastActivity = ChronoUnit.DAYS.between(mostRecent, Instant.now());

        if (daysSinceLastActivity > 90) {
            double score = Math.min(0.85, 0.6 + (daysSinceLastActivity / 365.0));
            return new ScoringResult("DORMANCY_BREAK", score,
                    String.format("Dormancy break detected: employee %s had no activity for %d days",
                            employeeId, daysSinceLastActivity));
        }

        return new ScoringResult("DORMANCY_BREAK", 0.0, "Recent activity within 90 days");
    }

    public record ScoringResult(String anomalyType, double score, String ruleExplanation) {}
}
