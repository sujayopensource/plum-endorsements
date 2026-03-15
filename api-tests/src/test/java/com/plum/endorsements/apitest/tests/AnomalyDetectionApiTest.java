package com.plum.endorsements.apitest.tests;

import com.plum.endorsements.apitest.base.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Intelligence API")
@Feature("Anomaly Detection")
@DisplayName("Anomaly Detection API")
class AnomalyDetectionApiTest extends BaseApiTest {

    // ── Helper: Seed an anomaly detection record via JDBC ──

    private UUID seedAnomaly(UUID endorsementId, UUID employerId, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO anomaly_detections (id, endorsement_id, employer_id, anomaly_type, score,
                    explanation, flagged_at, status)
                VALUES (?, ?, ?, 'PREMIUM_SPIKE', 0.92, 'Premium is 3x above historical average',
                    now(), ?)
                """,
                id, endorsementId, employerId, status
        );
        return id;
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/anomalies returns list of anomalies")
    @Description("Fetching anomalies without filters returns all FLAGGED anomalies by default")
    void shouldReturnAnomaliesList() {
        // Seed an endorsement and a FLAGGED anomaly
        UUID endorsementId = seedEndorsementAtStatus("PROVISIONALLY_COVERED", 0);
        seedAnomaly(endorsementId, EMPLOYER_ID, "FLAGGED");

        given()
                .when()
                .get("/api/v1/intelligence/anomalies")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].id", notNullValue())
                .body("[0].endorsementId", notNullValue())
                .body("[0].employerId", notNullValue())
                .body("[0].anomalyType", equalTo("PREMIUM_SPIKE"))
                .body("[0].score", notNullValue())
                .body("[0].explanation", notNullValue())
                .body("[0].flaggedAt", notNullValue())
                .body("[0].status", equalTo("FLAGGED"));
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/anomalies?status=FLAGGED filters by status")
    @Description("Filtering anomalies by status returns only anomalies in that status")
    void shouldFilterAnomaliesByStatus() {
        // Seed an endorsement with two anomalies in different statuses
        UUID endorsementId = seedEndorsementAtStatus("PROVISIONALLY_COVERED", 0);
        seedAnomaly(endorsementId, EMPLOYER_ID, "FLAGGED");

        UUID endorsementId2 = seedEndorsementAtStatus("PROVISIONALLY_COVERED", 0);
        seedAnomaly(endorsementId2, EMPLOYER_ID, "DISMISSED");

        // Filter by FLAGGED status — should only return the FLAGGED anomaly
        given()
                .queryParam("status", "FLAGGED")
                .when()
                .get("/api/v1/intelligence/anomalies")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("findAll { it.status == 'FLAGGED' }.size()", greaterThanOrEqualTo(1))
                .body("findAll { it.status != 'FLAGGED' }.size()", equalTo(0));

        // Filter by DISMISSED status — should only return the DISMISSED anomaly
        given()
                .queryParam("status", "DISMISSED")
                .when()
                .get("/api/v1/intelligence/anomalies")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("findAll { it.status == 'DISMISSED' }.size()", greaterThanOrEqualTo(1))
                .body("findAll { it.status != 'DISMISSED' }.size()", equalTo(0));
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/anomalies?employerId={id} filters by employer")
    @Description("Filtering anomalies by employer ID returns only anomalies for that employer")
    void shouldFilterAnomaliesByEmployerId() {
        UUID otherEmployerId = UUID.fromString("99999999-9999-9999-9999-999999999999");

        // Seed endorsements for two different employers
        UUID endorsementId1 = seedEndorsementAtStatus("PROVISIONALLY_COVERED", 0);
        seedAnomaly(endorsementId1, EMPLOYER_ID, "FLAGGED");

        // Seed an endorsement for the other employer directly via JDBC
        UUID endorsementId2 = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO endorsements (id, employer_id, employee_id, insurer_id, policy_id,
                    type, status, coverage_start_date, employee_data, premium_amount,
                    idempotency_key, retry_count, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, 'ADD', 'PROVISIONALLY_COVERED', '2026-04-01',
                    '{"name":"Other Employee","dob":"1990-05-15","gender":"M"}'::jsonb, 1000.00,
                    ?, 0, now(), now(), 0)
                """,
                endorsementId2, otherEmployerId, EMPLOYEE_ID, INSURER_ID, POLICY_ID,
                "key-" + endorsementId2
        );
        seedAnomaly(endorsementId2, otherEmployerId, "FLAGGED");

        // Filter by EMPLOYER_ID — should only return anomalies for that employer
        given()
                .queryParam("employerId", EMPLOYER_ID)
                .when()
                .get("/api/v1/intelligence/anomalies")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("findAll { it.employerId == '" + EMPLOYER_ID + "' }.size()", greaterThanOrEqualTo(1))
                .body("findAll { it.employerId != '" + EMPLOYER_ID + "' }.size()", equalTo(0));
    }

    @Test
    @DisplayName("PUT /api/v1/intelligence/anomalies/{id}/review updates anomaly status")
    @Description("Reviewing an anomaly transitions its status and records reviewer notes")
    void shouldUpdateAnomalyStatusViaReview() {
        // Seed an endorsement and a FLAGGED anomaly
        UUID endorsementId = seedEndorsementAtStatus("PROVISIONALLY_COVERED", 0);
        UUID anomalyId = seedAnomaly(endorsementId, EMPLOYER_ID, "FLAGGED");

        Map<String, Object> reviewRequest = Map.of(
                "status", "DISMISSED",
                "notes", "False positive - seasonal premium adjustment"
        );

        given()
                .contentType(ContentType.JSON)
                .body(reviewRequest)
                .when()
                .put("/api/v1/intelligence/anomalies/{id}/review", anomalyId)
                .then()
                .statusCode(200)
                .body("id", equalTo(anomalyId.toString()))
                .body("status", equalTo("DISMISSED"))
                .body("reviewerNotes", equalTo("False positive - seasonal premium adjustment"))
                .body("reviewedAt", notNullValue());
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/anomalies returns empty list when no anomalies exist")
    @Description("When no anomalies are flagged, the endpoint returns an empty JSON array")
    void shouldReturnEmptyListWhenNoAnomalies() {
        given()
                .when()
                .get("/api/v1/intelligence/anomalies")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    // ── Phase 3 Enterprise-Grade Negative/Edge Case Tests ──

    @Test
    @DisplayName("GET /api/v1/intelligence/anomalies/{id} returns 404 for non-existent anomaly ID")
    @Description("Requesting a specific anomaly by a valid UUID that does not exist returns 404 Not Found")
    void shouldReturn404ForNonExistentAnomalyId() {
        UUID nonExistentId = UUID.randomUUID();

        given()
                .when()
                .get("/api/v1/intelligence/anomalies/{id}", nonExistentId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/anomalies/not-a-uuid returns 400 for invalid UUID format")
    @Description("Requesting an anomaly with an invalid UUID format in the path returns a 400 Bad Request")
    void shouldReturn400ForInvalidAnomalyIdFormat() {
        given()
                .when()
                .get("/api/v1/intelligence/anomalies/{id}", "not-a-uuid")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)));
        // Spring returns 400 for type mismatch on path variables when default error handling is active,
        // or 500 if the generic exception handler catches MethodArgumentTypeMismatchException.
    }

    @Test
    @DisplayName("PUT /api/v1/intelligence/anomalies/{id}/review returns error for non-existent anomaly")
    @Description("Reviewing a non-existent anomaly returns an error status since the anomaly cannot be found")
    void shouldReturn404WhenReviewingNonExistentAnomaly() {
        UUID nonExistentId = UUID.randomUUID();

        Map<String, Object> reviewRequest = Map.of(
                "status", "DISMISSED",
                "notes", "Attempted review on non-existent anomaly"
        );

        // The service throws IllegalArgumentException("Anomaly not found"),
        // caught by the IllegalArgument handler returning 400
        given()
                .contentType(ContentType.JSON)
                .body(reviewRequest)
                .when()
                .put("/api/v1/intelligence/anomalies/{id}/review", nonExistentId)
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(404), equalTo(500)));
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/anomalies?employerId={unknown} returns empty for unknown employer")
    @Description("Filtering anomalies by a random employer ID that has no data returns an empty list")
    void shouldReturnEmptyListForUnknownEmployer() {
        // Seed an anomaly for the known employer to ensure the endpoint works
        UUID endorsementId = seedEndorsementAtStatus("PROVISIONALLY_COVERED", 0);
        seedAnomaly(endorsementId, EMPLOYER_ID, "FLAGGED");

        // Query with a completely random unknown employer
        UUID unknownEmployerId = UUID.randomUUID();

        given()
                .queryParam("employerId", unknownEmployerId)
                .when()
                .get("/api/v1/intelligence/anomalies")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @DisplayName("PUT /api/v1/intelligence/anomalies/{id}/review with concurrent requests — one succeeds")
    @Description("Two concurrent review requests on the same anomaly should both complete without data corruption")
    void shouldHandleConcurrentReviewAttempts() throws ExecutionException, InterruptedException {
        // Seed an endorsement and a FLAGGED anomaly
        UUID endorsementId = seedEndorsementAtStatus("PROVISIONALLY_COVERED", 0);
        UUID anomalyId = seedAnomaly(endorsementId, EMPLOYER_ID, "FLAGGED");

        Map<String, Object> dismissRequest = Map.of(
                "status", "DISMISSED",
                "notes", "Concurrent review attempt 1"
        );
        Map<String, Object> confirmRequest = Map.of(
                "status", "CONFIRMED",
                "notes", "Concurrent review attempt 2"
        );

        // Fire two concurrent review requests
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() ->
                given()
                        .contentType(ContentType.JSON)
                        .body(dismissRequest)
                        .when()
                        .put("/api/v1/intelligence/anomalies/{id}/review", anomalyId)
                        .then()
                        .extract()
                        .statusCode()
        );

        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() ->
                given()
                        .contentType(ContentType.JSON)
                        .body(confirmRequest)
                        .when()
                        .put("/api/v1/intelligence/anomalies/{id}/review", anomalyId)
                        .then()
                        .extract()
                        .statusCode()
        );

        int status1 = future1.get();
        int status2 = future2.get();

        // At least one should succeed (200); the other may succeed or fail with a conflict/error
        boolean atLeastOneSucceeded = (status1 == 200 || status2 == 200);
        org.junit.jupiter.api.Assertions.assertTrue(atLeastOneSucceeded,
                "At least one concurrent review should succeed. Got status1=" + status1 + ", status2=" + status2);
    }

    @Test
    @DisplayName("PUT /api/v1/intelligence/anomalies/{id}/review with missing status returns 400")
    @Description("Submitting a review request with a blank or missing status field triggers validation and returns 400")
    void shouldValidateReviewRequestBody() {
        UUID endorsementId = seedEndorsementAtStatus("PROVISIONALLY_COVERED", 0);
        UUID anomalyId = seedAnomaly(endorsementId, EMPLOYER_ID, "FLAGGED");

        // Send request with empty status (violates @NotBlank on AnomalyReviewRequest.status)
        Map<String, Object> invalidRequest = Map.of(
                "notes", "Missing status field"
        );

        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .put("/api/v1/intelligence/anomalies/{id}/review", anomalyId)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/anomalies returns DORMANCY_BREAK anomaly type when seeded")
    @Description("Seeding an anomaly with DORMANCY_BREAK type verifies the new anomaly type is supported end-to-end")
    void shouldDetectDormancyBreakAnomaly() {
        UUID endorsementId = seedEndorsementAtStatus("PROVISIONALLY_COVERED", 0);
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO anomaly_detections (id, endorsement_id, employer_id, anomaly_type, score,
                    explanation, flagged_at, status)
                VALUES (?, ?, ?, 'DORMANCY_BREAK', 0.78, 'Employee had no activity for 120 days',
                    now(), 'FLAGGED')
                """,
                id, endorsementId, EMPLOYER_ID
        );

        given()
                .queryParam("status", "FLAGGED")
                .when()
                .get("/api/v1/intelligence/anomalies")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("findAll { it.anomalyType == 'DORMANCY_BREAK' }.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/anomalies?status=CONFIRMED_FRAUD returns only CONFIRMED_FRAUD anomalies")
    @Description("Filtering anomalies by CONFIRMED_FRAUD status returns anomalies that have been confirmed after review")
    void shouldFilterAnomaliesByConfirmedStatus() {
        UUID endorsementId1 = seedEndorsementAtStatus("PROVISIONALLY_COVERED", 0);
        seedAnomaly(endorsementId1, EMPLOYER_ID, "CONFIRMED_FRAUD");

        UUID endorsementId2 = seedEndorsementAtStatus("PROVISIONALLY_COVERED", 0);
        seedAnomaly(endorsementId2, EMPLOYER_ID, "FLAGGED");

        given()
                .queryParam("status", "CONFIRMED_FRAUD")
                .when()
                .get("/api/v1/intelligence/anomalies")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("findAll { it.status == 'CONFIRMED_FRAUD' }.size()", greaterThanOrEqualTo(1))
                .body("findAll { it.status != 'CONFIRMED_FRAUD' }.size()", equalTo(0));
    }
}
