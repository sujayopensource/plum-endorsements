package com.plum.endorsements.bdd.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Database helper for test data setup and cleanup.
 * Mirrors the helpers in api-tests BaseApiTest.
 */
@Component
public class DatabaseHelper {

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * Clean all tables in FK-safe order.
     */
    public void cleanDatabase() {
        // Phase 4 tables
        jdbc.execute("DELETE FROM audit_logs");
        // Archive tables
        jdbc.execute("DELETE FROM endorsement_events_archive");
        jdbc.execute("DELETE FROM endorsements_archive");
        // Phase 3: Intelligence tables
        jdbc.execute("DELETE FROM stp_rate_snapshots");
        jdbc.execute("DELETE FROM anomaly_detections");
        jdbc.execute("DELETE FROM balance_forecasts");
        jdbc.execute("DELETE FROM error_resolutions");
        jdbc.execute("DELETE FROM process_mining_metrics");
        // Phase 1-2 tables
        jdbc.execute("DELETE FROM reconciliation_items");
        jdbc.execute("DELETE FROM reconciliation_runs");
        jdbc.execute("DELETE FROM endorsement_events");
        jdbc.execute("DELETE FROM ea_transactions");
        jdbc.execute("DELETE FROM provisional_coverages");
        jdbc.execute("DELETE FROM endorsements");
        jdbc.execute("DELETE FROM endorsement_batches");
        jdbc.execute("DELETE FROM ea_accounts");
    }

    /**
     * Seed an EA account with the given balance.
     */
    public void seedEAAccount(UUID employerId, UUID insurerId, BigDecimal balance) {
        jdbc.update(
                "INSERT INTO ea_accounts (employer_id, insurer_id, balance, reserved, updated_at) VALUES (?, ?, ?, 0, now()) ON CONFLICT (employer_id, insurer_id) DO UPDATE SET balance = EXCLUDED.balance, updated_at = now()",
                employerId, insurerId, balance
        );
    }

    /**
     * Seed an endorsement directly at a specific status via JDBC.
     */
    public UUID seedEndorsementAtStatus(UUID employerId, UUID employeeId, UUID insurerId, UUID policyId,
                                         String status, int retryCount) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO endorsements (id, employer_id, employee_id, insurer_id, policy_id,
                    type, status, coverage_start_date, employee_data, premium_amount,
                    idempotency_key, retry_count, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, 'ADD', ?, '2026-04-01',
                    '{"name":"Test Employee","dob":"1990-05-15","gender":"M"}'::jsonb,
                    1000.00, ?, ?, now(), now(), 0)
                """,
                id, employerId, employeeId, insurerId, policyId,
                status, "key-" + id, retryCount
        );
        return id;
    }

    /**
     * Seed an endorsement error (rejection details) for a given endorsement.
     * Used by error resolution BDD scenarios.
     */
    public void seedEndorsementError(UUID endorsementId, String errorCode, String errorMessage) {
        String eventDataJson = String.format(
                "{\"errorCode\":\"%s\",\"errorMessage\":\"%s\",\"statusFrom\":\"SUBMITTED\",\"statusTo\":\"REJECTED\",\"employeeData\":{\"dob\":\"03-07-1990\"}}",
                errorCode, errorMessage);
        jdbc.update("""
                INSERT INTO endorsement_events (endorsement_id, event_type, event_data, created_at)
                VALUES (?, 'REJECTION', ?::jsonb, now())
                """,
                endorsementId, eventDataJson
        );
    }

    /**
     * Seed an anomaly detection record directly in the database.
     * Used when anomaly detection via the API does not produce results deterministically.
     */
    public UUID seedAnomaly(UUID endorsementId, UUID employerId, String anomalyType, double score, String status) {
        // Ensure the endorsement exists (FK constraint on anomaly_detections.endorsement_id)
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM endorsements WHERE id = ?", Integer.class, endorsementId);
        if (count == null || count == 0) {
            // Seed a placeholder endorsement to satisfy the FK constraint
            UUID insurerId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            UUID policyId = UUID.fromString("33333333-3333-3333-3333-333333333333");
            endorsementId = seedEndorsementAtStatus(employerId, UUID.randomUUID(), insurerId, policyId, "CREATED", 0);
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO anomaly_detections (id, endorsement_id, employer_id, anomaly_type, score, explanation, flagged_at, status)
                VALUES (?, ?, ?, ?, ?, 'Seeded for testing', now(), ?)
                """,
                id, endorsementId, employerId, anomalyType, score, status);
        return id;
    }

    /**
     * Seed an endorsement with a created_at date in the past (daysAgo).
     */
    public UUID seedEndorsementWithCreatedAt(UUID employerId, UUID employeeId, UUID insurerId, UUID policyId,
                                              String status, int daysAgo) {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now().minus(daysAgo, ChronoUnit.DAYS);
        jdbc.update("""
                INSERT INTO endorsements (id, employer_id, employee_id, insurer_id, policy_id,
                    type, status, coverage_start_date, employee_data, premium_amount,
                    idempotency_key, retry_count, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, 'ADD', ?, '2026-04-01',
                    '{"name":"Test Employee","dob":"1990-05-15","gender":"M"}'::jsonb,
                    1000.00, ?, 0, ?, ?, 0)
                """,
                id, employerId, employeeId, insurerId, policyId,
                status, "key-" + id,
                Timestamp.from(createdAt), Timestamp.from(createdAt)
        );
        return id;
    }

    /**
     * Seed an insurer configuration entry for BDD tests with custom insurers.
     */
    public void seedInsurerConfiguration(UUID insurerId, String insurerName, String insurerCode) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM insurer_configurations WHERE insurer_id = ?", Integer.class, insurerId);
        if (count != null && count > 0) return; // Already exists
        jdbc.update("""
                INSERT INTO insurer_configurations (insurer_id, insurer_name, insurer_code, adapter_type,
                    supports_real_time, supports_batch, max_batch_size, batch_sla_hours, rate_limit_per_min,
                    data_format, auth_type, active)
                VALUES (?, ?, ?, 'MOCK', true, true, 100, 24, 60, 'JSON', 'NONE', true)
                ON CONFLICT (insurer_code) DO NOTHING
                """,
                insurerId, insurerName, insurerCode
        );
    }

    /**
     * Seed an STP rate snapshot for a given insurer at a given number of days ago.
     */
    public void seedStpRateSnapshot(UUID insurerId, int daysAgo, int total, int stp, double rate) {
        jdbc.update("""
                INSERT INTO stp_rate_snapshots (id, insurer_id, snapshot_date, total_endorsements,
                    stp_endorsements, stp_rate, created_at)
                VALUES (?, ?, CURRENT_DATE - ?, ?, ?, ?, now())
                """,
                UUID.randomUUID(), insurerId, daysAgo, total, stp, rate
        );
    }

    /**
     * Seed an error resolution with an outcome (SUCCESS or FAILURE).
     */
    public void seedErrorResolutionWithOutcome(UUID endorsementId, boolean autoApplied,
                                                double confidence, String outcome,
                                                String endorsementStatus) {
        jdbc.update("""
                INSERT INTO error_resolutions (id, endorsement_id, error_type, original_value,
                    corrected_value, resolution, confidence, auto_applied, created_at,
                    outcome, outcome_at, outcome_endorsement_status)
                VALUES (?, ?, 'DATE_FORMAT_MISMATCH', '15/03/2026', '2026-03-15',
                    'Converted DD/MM/YYYY to ISO 8601 format', ?, ?, now(), ?, now(), ?)
                """,
                UUID.randomUUID(), endorsementId, confidence, autoApplied, outcome, endorsementStatus
        );
    }

    /**
     * Seed a lifecycle transition event between two statuses with a given duration.
     * Used by process mining BDD scenarios.
     */
    public void seedLifecycleTransition(UUID endorsementId, String fromStatus, String toStatus,
                                         int durationMinutes) {
        Instant transitionEnd = Instant.now();
        Instant transitionStart = transitionEnd.minus(durationMinutes, ChronoUnit.MINUTES);

        String eventDataJson = String.format(
                "{\"statusFrom\":\"%s\",\"statusTo\":\"%s\",\"durationMinutes\":%d,\"transitionStart\":\"%s\"}",
                fromStatus, toStatus, durationMinutes, Timestamp.from(transitionStart).toString());
        jdbc.update("""
                INSERT INTO endorsement_events (endorsement_id, event_type, event_data, created_at)
                VALUES (?, 'STATUS_CHANGE', ?::jsonb, ?)
                """,
                endorsementId, eventDataJson,
                Timestamp.from(transitionEnd)
        );
    }

    /**
     * Seed a full happy-path lifecycle for an endorsement:
     * PROVISIONALLY_COVERED -> QUEUED_FOR_BATCH -> BATCH_SUBMITTED -> CONFIRMED
     */
    public void seedHappyPathLifecycle(UUID endorsementId) {
        seedLifecycleTransition(endorsementId, "PROVISIONALLY_COVERED", "QUEUED_FOR_BATCH", 5);
        seedLifecycleTransition(endorsementId, "QUEUED_FOR_BATCH", "BATCH_SUBMITTED", 15);
        seedLifecycleTransition(endorsementId, "BATCH_SUBMITTED", "CONFIRMED", 60);
    }

    /**
     * Seed a deviated lifecycle for an endorsement (includes a retry):
     * PROVISIONALLY_COVERED -> QUEUED_FOR_BATCH -> BATCH_SUBMITTED -> REJECTED -> QUEUED_FOR_BATCH -> BATCH_SUBMITTED -> CONFIRMED
     */
    public void seedDeviatedLifecycle(UUID endorsementId) {
        seedLifecycleTransition(endorsementId, "PROVISIONALLY_COVERED", "QUEUED_FOR_BATCH", 5);
        seedLifecycleTransition(endorsementId, "QUEUED_FOR_BATCH", "BATCH_SUBMITTED", 15);
        seedLifecycleTransition(endorsementId, "BATCH_SUBMITTED", "REJECTED", 120);
        seedLifecycleTransition(endorsementId, "REJECTED", "QUEUED_FOR_BATCH", 30);
        seedLifecycleTransition(endorsementId, "QUEUED_FOR_BATCH", "BATCH_SUBMITTED", 15);
        seedLifecycleTransition(endorsementId, "BATCH_SUBMITTED", "CONFIRMED", 60);
    }
}
