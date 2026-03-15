package com.plum.endorsements.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public sealed interface EndorsementEvent {

    UUID endorsementId();

    Instant occurredAt();

    UUID employerId();

    String eventType();

    record Created(UUID endorsementId, Instant occurredAt, UUID employerId, UUID employeeId,
                   EndorsementType type) implements EndorsementEvent {
        public String eventType() { return "ENDORSEMENT_CREATED"; }
    }

    record Validated(UUID endorsementId, Instant occurredAt,
                     UUID employerId) implements EndorsementEvent {
        public String eventType() { return "ENDORSEMENT_VALIDATED"; }
    }

    record ProvisionalCoverageGranted(UUID endorsementId, Instant occurredAt, UUID employerId,
                                      UUID employeeId, LocalDate coverageStart) implements EndorsementEvent {
        public String eventType() { return "PROVISIONAL_COVERAGE_GRANTED"; }
    }

    record SubmittedRealtime(UUID endorsementId, Instant occurredAt, UUID employerId,
                             UUID insurerId) implements EndorsementEvent {
        public String eventType() { return "ENDORSEMENT_SUBMITTED_REALTIME"; }
    }

    record QueuedForBatch(UUID endorsementId, Instant occurredAt,
                          UUID employerId) implements EndorsementEvent {
        public String eventType() { return "ENDORSEMENT_QUEUED_FOR_BATCH"; }
    }

    record BatchSubmitted(UUID endorsementId, Instant occurredAt, UUID employerId,
                          UUID batchId) implements EndorsementEvent {
        public String eventType() { return "BATCH_SUBMITTED"; }
    }

    record InsurerProcessing(UUID endorsementId, Instant occurredAt, UUID employerId,
                             String insurerReference) implements EndorsementEvent {
        public String eventType() { return "INSURER_PROCESSING"; }
    }

    record Confirmed(UUID endorsementId, Instant occurredAt, UUID employerId,
                     String insurerReference) implements EndorsementEvent {
        public String eventType() { return "ENDORSEMENT_CONFIRMED"; }
    }

    record Rejected(UUID endorsementId, Instant occurredAt, UUID employerId,
                    String reason) implements EndorsementEvent {
        public String eventType() { return "ENDORSEMENT_REJECTED"; }
    }

    record RetryScheduled(UUID endorsementId, Instant occurredAt, UUID employerId,
                          int attemptNumber) implements EndorsementEvent {
        public String eventType() { return "ENDORSEMENT_RETRY_SCHEDULED"; }
    }

    record FailedPermanent(UUID endorsementId, Instant occurredAt, UUID employerId,
                           String reason) implements EndorsementEvent {
        public String eventType() { return "ENDORSEMENT_FAILED_PERMANENT"; }
    }

    record EADebited(UUID endorsementId, Instant occurredAt, UUID employerId,
                     BigDecimal amount) implements EndorsementEvent {
        public String eventType() { return "EA_DEBITED"; }
    }

    record EACredited(UUID endorsementId, Instant occurredAt, UUID employerId,
                      BigDecimal amount) implements EndorsementEvent {
        public String eventType() { return "EA_CREDITED"; }
    }

    // --- Reconciliation events ---

    record ReconciliationMatched(UUID endorsementId, Instant occurredAt, UUID employerId,
                                  String insurerReference) implements EndorsementEvent {
        public String eventType() { return "RECONCILIATION_MATCHED"; }
    }

    record ReconciliationDiscrepancy(UUID endorsementId, Instant occurredAt, UUID employerId,
                                      String details) implements EndorsementEvent {
        public String eventType() { return "RECONCILIATION_DISCREPANCY"; }
    }

    record ReconciliationMissing(UUID endorsementId, Instant occurredAt, UUID employerId,
                                  UUID insurerId) implements EndorsementEvent {
        public String eventType() { return "RECONCILIATION_MISSING"; }
    }

    record BalanceForecastAlert(UUID endorsementId, Instant occurredAt, UUID employerId,
                                 BigDecimal shortfall) implements EndorsementEvent {
        public String eventType() { return "BALANCE_FORECAST_ALERT"; }
    }

    // --- Intelligence events (Phase 3) ---

    record AnomalyDetected(UUID endorsementId, Instant occurredAt, UUID employerId,
                            String anomalyType, double anomalyScore, String explanation)
            implements EndorsementEvent {
        public String eventType() { return "ANOMALY_DETECTED"; }
    }

    record ForecastGenerated(UUID endorsementId, Instant occurredAt, UUID employerId,
                              BigDecimal forecastedNeed, int daysAhead, String narrative)
            implements EndorsementEvent {
        public String eventType() { return "FORECAST_GENERATED"; }
    }

    record BatchOptimized(UUID endorsementId, Instant occurredAt, UUID employerId,
                           UUID batchId, String optimizationStrategy, BigDecimal savedAmount)
            implements EndorsementEvent {
        public String eventType() { return "BATCH_OPTIMIZED"; }
    }

    record ErrorAutoResolved(UUID endorsementId, Instant occurredAt, UUID employerId,
                              String errorType, String resolution, boolean autoApplied)
            implements EndorsementEvent {
        public String eventType() { return "ERROR_AUTO_RESOLVED"; }
    }

    record ErrorResolutionSuggested(UUID endorsementId, Instant occurredAt, UUID employerId,
                                      String errorType, String suggestedFix, double confidence)
            implements EndorsementEvent {
        public String eventType() { return "ERROR_RESOLUTION_SUGGESTED"; }
    }

    record ProcessMiningInsight(UUID endorsementId, Instant occurredAt, UUID employerId,
                                 String insightType, String insight)
            implements EndorsementEvent {
        public String eventType() { return "PROCESS_MINING_INSIGHT"; }
    }

    // --- Coverage lifecycle events ---

    record ProvisionalCoverageExpired(UUID endorsementId, Instant occurredAt, UUID employerId,
                                       UUID employeeId) implements EndorsementEvent {
        public String eventType() { return "PROVISIONAL_COVERAGE_EXPIRED"; }
    }

    record ProvisionalCoverageConfirmed(UUID endorsementId, Instant occurredAt, UUID employerId,
                                         UUID employeeId) implements EndorsementEvent {
        public String eventType() { return "PROVISIONAL_COVERAGE_CONFIRMED"; }
    }
}
