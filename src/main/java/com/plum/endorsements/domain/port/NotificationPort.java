package com.plum.endorsements.domain.port;

import java.util.UUID;

public interface NotificationPort {
    void notifyEndorsementConfirmed(UUID employerId, UUID endorsementId);
    void notifyEndorsementRejected(UUID employerId, UUID endorsementId, String reason);
    void notifyInsufficientBalance(UUID employerId, java.math.BigDecimal required, java.math.BigDecimal available);
    void notifyBatchSlaBreached(UUID batchId, UUID insurerId);
    void notifyReconciliationDiscrepancy(UUID insurerId, UUID endorsementId, String details);
    void notifyReconciliationComplete(UUID insurerId, UUID runId, int matched, int discrepancies);
    void notifyAnomalyDetected(UUID employerId, String anomalyType, double score, String explanation);
    void notifyForecastShortfall(UUID employerId, java.math.BigDecimal shortfall, int daysUntil);
    void notifyCoverageAtRisk(UUID employerId, UUID endorsementId, String reason);
    void notifyCoverageExpired(UUID employerId, UUID employeeId, String reason);
}
