package com.plum.endorsements.infrastructure.notification;

import com.plum.endorsements.domain.port.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnMissingBean(WebhookNotificationAdapter.class)
public class LoggingNotificationAdapter implements NotificationPort {

    @Override
    public void notifyEndorsementConfirmed(UUID employerId, UUID endorsementId) {
        log.info("Endorsement {} confirmed for employer {}", endorsementId, employerId);
    }

    @Override
    public void notifyEndorsementRejected(UUID employerId, UUID endorsementId, String reason) {
        log.info("Endorsement {} rejected for employer {}: {}", endorsementId, employerId, reason);
    }

    @Override
    public void notifyInsufficientBalance(UUID employerId, BigDecimal required, BigDecimal available) {
        log.info("Insufficient balance for employer {}: required={}, available={}", employerId, required, available);
    }

    @Override
    public void notifyBatchSlaBreached(UUID batchId, UUID insurerId) {
        log.info("Batch {} SLA breached for insurer {}", batchId, insurerId);
    }

    @Override
    public void notifyReconciliationDiscrepancy(UUID insurerId, UUID endorsementId, String details) {
        log.warn("Reconciliation discrepancy for insurer {}, endorsement {}: {}",
                insurerId, endorsementId, details);
    }

    @Override
    public void notifyReconciliationComplete(UUID insurerId, UUID runId, int matched, int discrepancies) {
        log.info("Reconciliation run {} for insurer {} completed: {} matched, {} discrepancies",
                runId, insurerId, matched, discrepancies);
    }

    @Override
    public void notifyAnomalyDetected(UUID employerId, String anomalyType, double score, String explanation) {
        log.warn("ANOMALY DETECTED for employer {}: type={}, score={}, explanation={}",
                employerId, anomalyType, score, explanation);
    }

    @Override
    public void notifyForecastShortfall(UUID employerId, BigDecimal shortfall, int daysUntil) {
        log.warn("FORECAST SHORTFALL for employer {}: shortfall={}, days until={}", employerId, shortfall, daysUntil);
    }

    @Override
    public void notifyCoverageAtRisk(UUID employerId, UUID endorsementId, String reason) {
        log.warn("COVERAGE AT RISK for employer {}, endorsement {}: {}",
                employerId, endorsementId, reason);
    }

    @Override
    public void notifyCoverageExpired(UUID employerId, UUID employeeId, String reason) {
        log.warn("COVERAGE EXPIRED for employer {}, employee {}: {}",
                employerId, employeeId, reason);
    }
}
