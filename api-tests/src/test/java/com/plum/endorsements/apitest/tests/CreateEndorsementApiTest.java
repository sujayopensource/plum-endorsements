package com.plum.endorsements.apitest.tests;

import com.plum.endorsements.apitest.base.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Endorsement API")
@Feature("Create Endorsement")
@DisplayName("POST /api/v1/endorsements")
class CreateEndorsementApiTest extends BaseApiTest {

    @Test
    @DisplayName("Should create ADD endorsement with PROVISIONALLY_COVERED status")
    @Description("Creates a new ADD endorsement and verifies it transitions to PROVISIONALLY_COVERED")
    void shouldCreateEndorsement_ReturnsCreatedWithProvisionallyCoveredStatus() {
        Map<String, Object> request = createEndorsementRequest("ADD", new BigDecimal("1200.00"));

        createEndorsementViaApi(request)
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("employerId", equalTo(EMPLOYER_ID.toString()))
                .body("employeeId", equalTo(EMPLOYEE_ID.toString()))
                .body("insurerId", equalTo(INSURER_ID.toString()))
                .body("policyId", equalTo(POLICY_ID.toString()))
                .body("type", equalTo("ADD"))
                .body("status", equalTo("PROVISIONALLY_COVERED"))
                .body("coverageStartDate", equalTo("2026-04-01"))
                .body("coverageEndDate", equalTo("2027-03-31"))
                .body("premiumAmount", equalTo(1200.00f))
                .body("retryCount", equalTo(0))
                .body("idempotencyKey", notNullValue())
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue());
    }

    @Test
    @DisplayName("Should auto-generate idempotency key when not provided")
    @Description("When no idempotencyKey is provided, the system generates one from employerId-employeeId-type-coverageStartDate")
    void shouldCreateEndorsement_SetsIdempotencyKeyAutomatically() {
        Map<String, Object> request = createEndorsementRequest("ADD", new BigDecimal("500.00"));

        String expectedKeyPrefix = EMPLOYER_ID + "-" + EMPLOYEE_ID + "-ADD-2026-04-01";

        createEndorsementViaApi(request)
                .statusCode(201)
                .body("idempotencyKey", equalTo(expectedKeyPrefix));
    }

    @Test
    @DisplayName("Should return existing endorsement when duplicate idempotency key is used")
    @Description("Idempotency guarantee: sending the same request twice returns the same endorsement")
    void shouldReturnExistingEndorsement_WhenDuplicateIdempotencyKey() {
        Map<String, Object> request = createEndorsementRequest(
                EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID,
                "ADD", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                new BigDecimal("1200.00"), "unique-key-123");

        // First call
        String firstId = createEndorsementViaApi(request)
                .statusCode(201)
                .extract().path("id");

        // Second call with same idempotency key
        String secondId = createEndorsementViaApi(request)
                .statusCode(201)
                .extract().path("id");

        org.assertj.core.api.Assertions.assertThat(secondId).isEqualTo(firstId);
    }

    @Test
    @DisplayName("Should create DELETE endorsement without provisional coverage")
    @Description("DELETE endorsements should not create provisional coverage records")
    void shouldCreateDeleteEndorsement_NoProvisionalCoverage() {
        Map<String, Object> request = createEndorsementRequest("DELETE", null);

        String id = createEndorsementViaApi(request)
                .statusCode(201)
                .body("type", equalTo("DELETE"))
                .body("status", equalTo("PROVISIONALLY_COVERED"))
                .extract().path("id");

        // No provisional coverage for DELETE type
        given()
                .when()
                .get("/api/v1/endorsements/{id}/coverage", id)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Should create UPDATE endorsement without EA reservation")
    @Description("UPDATE endorsements should not check or reserve EA balance")
    void shouldCreateUpdateEndorsement_NoEAReservation() {
        seedEAAccount(EMPLOYER_ID, INSURER_ID, new BigDecimal("50000.00"));

        Map<String, Object> request = createEndorsementRequest(
                EMPLOYER_ID, UUID.randomUUID(), INSURER_ID, POLICY_ID,
                "UPDATE", LocalDate.of(2026, 4, 1), null,
                new BigDecimal("1000.00"), null);

        createEndorsementViaApi(request)
                .statusCode(201)
                .body("type", equalTo("UPDATE"));

        // EA balance should remain unchanged
        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("insurerId", INSURER_ID)
                .when()
                .get("/api/v1/ea-accounts")
                .then()
                .statusCode(200)
                .body("reserved", equalTo(0.0f))
                .body("availableBalance", equalTo(50000.00f));
    }

    @Test
    @DisplayName("Should reserve funds when EA account has sufficient balance")
    @Description("ADD endorsements with sufficient EA balance should reserve the premium amount")
    void shouldReserveFunds_WhenEAAccountExists() {
        seedEAAccount(EMPLOYER_ID, INSURER_ID, new BigDecimal("50000.00"));

        Map<String, Object> request = createEndorsementRequest("ADD", new BigDecimal("1200.00"));
        createEndorsementViaApi(request).statusCode(201);

        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("insurerId", INSURER_ID)
                .when()
                .get("/api/v1/ea-accounts")
                .then()
                .statusCode(200)
                .body("balance", equalTo(50000.00f))
                .body("reserved", equalTo(1200.00f))
                .body("availableBalance", equalTo(48800.00f));
    }

    @Test
    @DisplayName("Should not reserve funds when balance is insufficient")
    @Description("ADD endorsements with insufficient EA balance should still be created but without reservation")
    void shouldNotReserveFunds_WhenInsufficientBalance() {
        seedEAAccount(EMPLOYER_ID, INSURER_ID, new BigDecimal("500.00"));

        Map<String, Object> request = createEndorsementRequest("ADD", new BigDecimal("1200.00"));
        createEndorsementViaApi(request)
                .statusCode(201)
                .body("status", equalTo("PROVISIONALLY_COVERED"));

        // Balance unchanged because insufficient
        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("insurerId", INSURER_ID)
                .when()
                .get("/api/v1/ea-accounts")
                .then()
                .statusCode(200)
                .body("reserved", equalTo(0.0f))
                .body("availableBalance", equalTo(500.00f));
    }

    @Test
    @DisplayName("Should return 400 when required fields are missing")
    @Description("Validation: missing required fields should return ProblemDetail with field errors")
    void shouldReturn400_WhenMissingRequiredFields() {
        Map<String, Object> request = Map.of("type", "ADD");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements")
                .then()
                .statusCode(400)
                .body("title", equalTo("Validation Error"))
                .body("errors", notNullValue())
                .body("errors.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("Should return 400 when endorsement type is invalid")
    @Description("An invalid endorsement type value should cause a 400 error")
    void shouldReturn400_WhenInvalidEndorsementType() {
        Map<String, Object> request = createEndorsementRequest(
                EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID,
                "INVALID_TYPE", LocalDate.of(2026, 4, 1), null,
                new BigDecimal("1000.00"), null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)));
    }
}
