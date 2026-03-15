package com.plum.endorsements.apitest.tests;

import com.plum.endorsements.apitest.base.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Endorsement API")
@Feature("End-to-End Lifecycle")
@DisplayName("Endorsement Lifecycle Tests")
class EndorsementLifecycleApiTest extends BaseApiTest {

    @Test
    @DisplayName("Should complete full lifecycle: create -> submit -> confirm")
    @Description("End-to-end test: creates an ADD endorsement with EA balance, submits to insurer " +
            "(auto-confirms via mock), and verifies final state including coverage confirmation")
    void shouldCompleteFullLifecycle_CreateSubmitConfirm() {
        // Seed EA account
        seedEAAccount(EMPLOYER_ID, INSURER_ID, new BigDecimal("50000.00"));

        // Step 1: Create endorsement
        Map<String, Object> request = createEndorsementRequest("ADD", new BigDecimal("1500.00"));
        String id = createEndorsementViaApi(request)
                .statusCode(201)
                .body("status", equalTo("PROVISIONALLY_COVERED"))
                .extract().path("id");

        // Step 2: Verify provisional coverage exists
        given()
                .when()
                .get("/api/v1/endorsements/{id}/coverage", id)
                .then()
                .statusCode(200)
                .body("endorsementId", equalTo(id));

        // Step 3: Verify EA reservation
        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("insurerId", INSURER_ID)
                .when()
                .get("/api/v1/ea-accounts")
                .then()
                .statusCode(200)
                .body("reserved", equalTo(1500.00f))
                .body("availableBalance", equalTo(48500.00f));

        // Step 4: Submit to insurer (auto-confirms via MockInsurerAdapter)
        given()
                .when()
                .post("/api/v1/endorsements/{id}/submit", id)
                .then()
                .statusCode(202);

        // Step 5: Verify endorsement is CONFIRMED
        given()
                .when()
                .get("/api/v1/endorsements/{id}", id)
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"))
                .body("insurerReference", startsWith("INS-RT-"));

        // Step 6: Verify provisional coverage is now CONFIRMED
        given()
                .when()
                .get("/api/v1/endorsements/{id}/coverage", id)
                .then()
                .statusCode(200)
                .body("coverageType", equalTo("CONFIRMED"))
                .body("confirmedAt", notNullValue());
    }

    @Test
    @DisplayName("Should verify all response fields match request on creation")
    @Description("Verifies every field in the endorsement response matches the original request values")
    void shouldCompleteFullLifecycle_CreateAndVerifyAllFields() {
        Map<String, Object> request = createEndorsementRequest(
                EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID,
                "ADD", java.time.LocalDate.of(2026, 6, 15), java.time.LocalDate.of(2027, 6, 14),
                new BigDecimal("2500.00"), "lifecycle-test-key-001");

        String id = createEndorsementViaApi(request)
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .get("/api/v1/endorsements/{id}", id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("employerId", equalTo(EMPLOYER_ID.toString()))
                .body("employeeId", equalTo(EMPLOYEE_ID.toString()))
                .body("insurerId", equalTo(INSURER_ID.toString()))
                .body("policyId", equalTo(POLICY_ID.toString()))
                .body("type", equalTo("ADD"))
                .body("status", equalTo("PROVISIONALLY_COVERED"))
                .body("coverageStartDate", equalTo("2026-06-15"))
                .body("coverageEndDate", equalTo("2027-06-14"))
                .body("premiumAmount", equalTo(2500.00f))
                .body("batchId", nullValue())
                .body("insurerReference", nullValue())
                .body("retryCount", equalTo(0))
                .body("failureReason", nullValue())
                .body("idempotencyKey", equalTo("lifecycle-test-key-001"))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue());
    }
}
