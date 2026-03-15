package com.plum.endorsements.apitest.tests;

import com.plum.endorsements.apitest.base.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Multi-Insurer API")
@Feature("Reconciliation")
@DisplayName("Reconciliation API")
class ReconciliationApiTest extends BaseApiTest {

    @Test
    @DisplayName("Should trigger reconciliation for insurer")
    @Description("POST /api/v1/reconciliation/trigger creates a reconciliation run with id, insurerId, status, totalChecked, and startedAt")
    void shouldTriggerReconciliation() {
        given()
                .queryParam("insurerId", INSURER_ID)
                .when()
                .post("/api/v1/reconciliation/trigger")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("insurerId", equalTo(INSURER_ID.toString()))
                .body("status", notNullValue())
                .body("totalChecked", greaterThanOrEqualTo(0))
                .body("startedAt", notNullValue());
    }

    @Test
    @DisplayName("Should get reconciliation runs for insurer")
    @Description("Triggers a reconciliation then verifies GET /api/v1/reconciliation/runs returns at least one run")
    void shouldGetReconciliationRuns() {
        // Trigger a reconciliation first
        given()
                .queryParam("insurerId", INSURER_ID)
                .when()
                .post("/api/v1/reconciliation/trigger")
                .then()
                .statusCode(200);

        // Verify runs list contains at least one entry
        given()
                .queryParam("insurerId", INSURER_ID)
                .when()
                .get("/api/v1/reconciliation/runs")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("Should get reconciliation run items")
    @Description("Seeds a CONFIRMED endorsement, triggers reconciliation, retrieves the run, then fetches its items")
    void shouldGetReconciliationRunItems() {
        // Seed an endorsement at CONFIRMED status so reconciliation has something to check
        seedEndorsementAtStatus("CONFIRMED", 0);

        // Trigger reconciliation
        String runId = given()
                .queryParam("insurerId", INSURER_ID)
                .when()
                .post("/api/v1/reconciliation/trigger")
                .then()
                .statusCode(200)
                .extract().path("id");

        // Get items for the run
        given()
                .when()
                .get("/api/v1/reconciliation/runs/{runId}/items", runId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("Should return empty runs for unknown insurer")
    @Description("GET /api/v1/reconciliation/runs with a random insurerId returns an empty list")
    void shouldReturnEmptyRunsForNewInsurer() {
        given()
                .queryParam("insurerId", UUID.randomUUID())
                .when()
                .get("/api/v1/reconciliation/runs")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }
}
