package com.plum.endorsements.apitest.tests;

import com.plum.endorsements.apitest.base.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Endorsement API")
@Feature("Batch Progress")
@DisplayName("GET /api/v1/endorsements/employers/{employerId}/batches")
class BatchProgressApiTest extends BaseApiTest {

    @Test
    @DisplayName("Should return empty page when no batches exist for employer")
    @Description("Batch progress endpoint returns empty page when employer has no batches")
    void shouldReturnEmptyPage_WhenNoBatches() {
        given()
                .when()
                .get("/api/v1/endorsements/employers/{employerId}/batches", EMPLOYER_ID)
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0))
                .body("totalElements", equalTo(0));
    }

    @Test
    @DisplayName("Should return batch progress for employer with batched endorsements")
    @Description("Creates a batch via JDBC, links endorsements, and verifies the batch progress response")
    void shouldReturnBatchProgress_WhenBatchExists() {
        // Create a batch
        UUID batchId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO endorsement_batches (id, insurer_id, status, endorsement_count, total_premium,
                    submitted_at, sla_deadline, insurer_batch_ref, created_at)
                VALUES (?, ?, 'SUBMITTED', 3, 4500.00, now(), ?, 'BATCH-REF-001', now())
                """,
                batchId, INSURER_ID,
                Timestamp.from(Instant.now().plusSeconds(86400)));

        // Create endorsements linked to the batch
        for (int i = 0; i < 3; i++) {
            UUID endorsementId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO endorsements (id, employer_id, employee_id, insurer_id, policy_id,
                        type, status, coverage_start_date, employee_data, premium_amount,
                        idempotency_key, retry_count, batch_id, created_at, updated_at, version)
                    VALUES (?, ?, ?, ?, ?, 'ADD', 'BATCH_SUBMITTED', '2026-04-01',
                        '{"name":"Test Employee"}'::jsonb, 1500.00, ?, 0, ?, now(), now(), 0)
                    """,
                    endorsementId, EMPLOYER_ID, UUID.randomUUID(), INSURER_ID, POLICY_ID,
                    "key-" + endorsementId, batchId);
        }

        given()
                .when()
                .get("/api/v1/endorsements/employers/{employerId}/batches", EMPLOYER_ID)
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].batchId", equalTo(batchId.toString()))
                .body("content[0].insurerId", equalTo(INSURER_ID.toString()))
                .body("content[0].status", equalTo("SUBMITTED"))
                .body("content[0].endorsementCount", equalTo(3))
                .body("content[0].insurerBatchRef", equalTo("BATCH-REF-001"));
    }

    @Test
    @DisplayName("Should return empty for employer with no linked endorsements")
    @Description("If a batch exists but has no endorsements linked to this employer, it should not be returned")
    void shouldReturnEmpty_WhenBatchExistsButNotLinkedToEmployer() {
        UUID otherEmployerId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        jdbc.update("""
                INSERT INTO endorsement_batches (id, insurer_id, status, endorsement_count, total_premium,
                    insurer_batch_ref, created_at)
                VALUES (?, ?, 'SUBMITTED', 1, 1000.00, 'BATCH-OTHER-001', now())
                """,
                batchId, INSURER_ID);

        // Endorsement belongs to a different employer
        UUID endorsementId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO endorsements (id, employer_id, employee_id, insurer_id, policy_id,
                    type, status, coverage_start_date, employee_data, premium_amount,
                    idempotency_key, retry_count, batch_id, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, 'ADD', 'BATCH_SUBMITTED', '2026-04-01',
                    '{"name":"Test Employee"}'::jsonb, 1000.00, ?, 0, ?, now(), now(), 0)
                """,
                endorsementId, otherEmployerId, UUID.randomUUID(), INSURER_ID, POLICY_ID,
                "key-" + endorsementId, batchId);

        given()
                .when()
                .get("/api/v1/endorsements/employers/{employerId}/batches", EMPLOYER_ID)
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0))
                .body("totalElements", equalTo(0));
    }

    @Test
    @DisplayName("Should paginate batch progress results")
    @Description("Batch progress endpoint respects page and size parameters")
    void shouldPaginateBatchProgress() {
        // Create 3 batches with linked endorsements
        for (int b = 0; b < 3; b++) {
            UUID batchId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO endorsement_batches (id, insurer_id, status, endorsement_count, total_premium,
                        insurer_batch_ref, created_at)
                    VALUES (?, ?, 'SUBMITTED', 1, 1000.00, ?, now())
                    """,
                    batchId, INSURER_ID, "BATCH-" + b);

            UUID endorsementId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO endorsements (id, employer_id, employee_id, insurer_id, policy_id,
                        type, status, coverage_start_date, employee_data, premium_amount,
                        idempotency_key, retry_count, batch_id, created_at, updated_at, version)
                    VALUES (?, ?, ?, ?, ?, 'ADD', 'BATCH_SUBMITTED', '2026-04-01',
                        '{"name":"Test Employee"}'::jsonb, 1000.00, ?, 0, ?, now(), now(), 0)
                    """,
                    endorsementId, EMPLOYER_ID, UUID.randomUUID(), INSURER_ID, POLICY_ID,
                    "key-" + endorsementId, batchId);
        }

        given()
                .queryParam("page", 0)
                .queryParam("size", 2)
                .when()
                .get("/api/v1/endorsements/employers/{employerId}/batches", EMPLOYER_ID)
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("totalElements", equalTo(3))
                .body("totalPages", equalTo(2));
    }
}
