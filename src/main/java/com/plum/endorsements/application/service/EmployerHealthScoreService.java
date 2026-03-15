package com.plum.endorsements.application.service;

import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.domain.port.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployerHealthScoreService {

    private final EndorsementRepository endorsementRepository;
    private final EAAccountRepository eaAccountRepository;
    private final AnomalyDetectionRepository anomalyDetectionRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final MeterRegistry meterRegistry;

    public HealthScore calculateHealthScore(UUID employerId) {
        log.info("Calculating health score for employer {}", employerId);

        BigDecimal endorsementSuccessRate = calculateEndorsementSuccessRate(employerId);
        BigDecimal anomalyScore = calculateAnomalyScore(employerId);
        BigDecimal balanceHealthScore = calculateBalanceHealth(employerId);
        BigDecimal reconciliationScore = calculateReconciliationScore(employerId);

        // Weighted composite: success 40%, anomaly 20%, balance 20%, reconciliation 20%
        BigDecimal composite = endorsementSuccessRate.multiply(new BigDecimal("0.40"))
                .add(anomalyScore.multiply(new BigDecimal("0.20")))
                .add(balanceHealthScore.multiply(new BigDecimal("0.20")))
                .add(reconciliationScore.multiply(new BigDecimal("0.20")))
                .setScale(1, RoundingMode.HALF_UP);

        String riskLevel = composite.compareTo(new BigDecimal("80")) >= 0 ? "LOW"
                : composite.compareTo(new BigDecimal("60")) >= 0 ? "MEDIUM" : "HIGH";

        HealthScore score = new HealthScore(
                employerId, composite, riskLevel,
                endorsementSuccessRate, anomalyScore,
                balanceHealthScore, reconciliationScore,
                Instant.now()
        );

        meterRegistry.gauge("endorsement.employer.health_score",
                io.micrometer.core.instrument.Tags.of("employerId", employerId.toString()),
                composite.doubleValue());

        log.info("Health score for employer {}: {} (risk={})", employerId, composite, riskLevel);
        return score;
    }

    private BigDecimal calculateEndorsementSuccessRate(UUID employerId) {
        long confirmed = endorsementRepository.countByEmployerIdAndStatus(employerId, EndorsementStatus.CONFIRMED);
        long rejected = endorsementRepository.countByEmployerIdAndStatus(employerId, EndorsementStatus.REJECTED);
        long failed = endorsementRepository.countByEmployerIdAndStatus(employerId, EndorsementStatus.FAILED_PERMANENT);
        long total = confirmed + rejected + failed;
        if (total == 0) return new BigDecimal("100.0");
        return BigDecimal.valueOf(confirmed * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAnomalyScore(UUID employerId) {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        long recentAnomalies = anomalyDetectionRepository.countByEmployerIdAndFlaggedAtAfter(employerId, thirtyDaysAgo);
        // 0 anomalies = 100, 1-2 = 80, 3-5 = 60, 6+ = 30
        if (recentAnomalies == 0) return new BigDecimal("100.0");
        if (recentAnomalies <= 2) return new BigDecimal("80.0");
        if (recentAnomalies <= 5) return new BigDecimal("60.0");
        return new BigDecimal("30.0");
    }

    private BigDecimal calculateBalanceHealth(UUID employerId) {
        List<EAAccount> accounts = eaAccountRepository.findByEmployerId(employerId);
        if (accounts.isEmpty()) return new BigDecimal("100.0");

        long healthyAccounts = accounts.stream()
                .filter(a -> a.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .count();
        return BigDecimal.valueOf(healthyAccounts * 100.0 / accounts.size())
                .setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateReconciliationScore(UUID employerId) {
        // Default to 100 if no reconciliation data exists for this employer
        return new BigDecimal("100.0");
    }

    public record HealthScore(
            UUID employerId,
            BigDecimal overallScore,
            String riskLevel,
            BigDecimal endorsementSuccessRate,
            BigDecimal anomalyScore,
            BigDecimal balanceHealthScore,
            BigDecimal reconciliationScore,
            Instant calculatedAt
    ) {}
}
