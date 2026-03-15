package com.plum.endorsements.apitest.tests;

import com.plum.endorsements.apitest.base.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Multi-Insurer API")
@Feature("Multi-Insurer Submission")
@DisplayName("Multi-Insurer Endorsement Submission")
class MultiInsurerSubmissionApiTest extends BaseApiTest {

    private static final UUID MULTI_INSURER_POLICY_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    @DisplayName("Should create endorsement for ICICI Lombard insurer")
    @Description("Creates an ADD endorsement targeting ICICI Lombard and verifies the insurerId in the response")
    void shouldCreateEndorsementForIciciInsurer() {
        seedEAAccount(EMPLOYER_ID, ICICI_INSURER_ID, new BigDecimal("50000.00"));

        Map<String, Object> request = createEndorsementRequest(
                EMPLOYER_ID, EMPLOYEE_ID, ICICI_INSURER_ID, MULTI_INSURER_POLICY_ID,
                "ADD", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                new BigDecimal("1200.00"), null);

        createEndorsementViaApi(request)
                .statusCode(201)
                .body("insurerId", equalTo(ICICI_INSURER_ID.toString()))
                .body("type", equalTo("ADD"))
                .body("status", equalTo("PROVISIONALLY_COVERED"));
    }

    @Test
    @DisplayName("Should create endorsement for Bajaj Allianz insurer")
    @Description("Creates an ADD endorsement targeting Bajaj Allianz and verifies the insurerId in the response")
    void shouldCreateEndorsementForBajajInsurer() {
        seedEAAccount(EMPLOYER_ID, BAJAJ_INSURER_ID, new BigDecimal("50000.00"));

        Map<String, Object> request = createEndorsementRequest(
                EMPLOYER_ID, EMPLOYEE_ID, BAJAJ_INSURER_ID, MULTI_INSURER_POLICY_ID,
                "ADD", LocalDate.of(2026, 5, 1), LocalDate.of(2027, 4, 30),
                new BigDecimal("1500.00"), null);

        createEndorsementViaApi(request)
                .statusCode(201)
                .body("insurerId", equalTo(BAJAJ_INSURER_ID.toString()))
                .body("type", equalTo("ADD"))
                .body("status", equalTo("PROVISIONALLY_COVERED"));
    }

    @Test
    @DisplayName("Should submit endorsement to ICICI via real-time path")
    @Description("Creates and submits an endorsement to ICICI Lombard, which auto-confirms via real-time " +
            "and sets an insurer reference starting with 'ICICI-'")
    void shouldSubmitEndorsementToIciciViaRealTime() {
        seedEAAccount(EMPLOYER_ID, ICICI_INSURER_ID, new BigDecimal("50000.00"));

        Map<String, Object> request = createEndorsementRequest(
                EMPLOYER_ID, EMPLOYEE_ID, ICICI_INSURER_ID, MULTI_INSURER_POLICY_ID,
                "ADD", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31),
                new BigDecimal("1000.00"), null);

        String id = createEndorsementViaApi(request)
                .statusCode(201)
                .extract().path("id");

        // Submit to ICICI Lombard (auto-confirms via real-time adapter)
        given()
                .when()
                .post("/api/v1/endorsements/{id}/submit", id)
                .then()
                .statusCode(202);

        // Verify status is CONFIRMED and insurer reference has ICICI prefix
        given()
                .when()
                .get("/api/v1/endorsements/{id}", id)
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"))
                .body("insurerReference", startsWith("ICICI-"));
    }

    @Test
    @DisplayName("Should route endorsements to correct insurer")
    @Description("Creates endorsements for both ICICI and Bajaj, verifies each has the correct insurerId")
    void shouldRouteToCorrectInsurer() {
        seedEAAccount(EMPLOYER_ID, ICICI_INSURER_ID, new BigDecimal("50000.00"));
        seedEAAccount(EMPLOYER_ID, BAJAJ_INSURER_ID, new BigDecimal("50000.00"));

        // Create ICICI endorsement
        Map<String, Object> iciciRequest = createEndorsementRequest(
                EMPLOYER_ID, EMPLOYEE_ID, ICICI_INSURER_ID, MULTI_INSURER_POLICY_ID,
                "ADD", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30),
                new BigDecimal("1000.00"), "route-icici-key");

        String iciciId = createEndorsementViaApi(iciciRequest)
                .statusCode(201)
                .extract().path("id");

        // Create Bajaj endorsement (different employee to avoid idempotency collision)
        UUID bajajEmployeeId = UUID.randomUUID();
        Map<String, Object> bajajRequest = createEndorsementRequest(
                EMPLOYER_ID, bajajEmployeeId, BAJAJ_INSURER_ID, MULTI_INSURER_POLICY_ID,
                "ADD", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30),
                new BigDecimal("2000.00"), "route-bajaj-key");

        String bajajId = createEndorsementViaApi(bajajRequest)
                .statusCode(201)
                .extract().path("id");

        // Verify ICICI endorsement has correct insurer
        given()
                .when()
                .get("/api/v1/endorsements/{id}", iciciId)
                .then()
                .statusCode(200)
                .body("insurerId", equalTo(ICICI_INSURER_ID.toString()));

        // Verify Bajaj endorsement has correct insurer
        given()
                .when()
                .get("/api/v1/endorsements/{id}", bajajId)
                .then()
                .statusCode(200)
                .body("insurerId", equalTo(BAJAJ_INSURER_ID.toString()));
    }
}
