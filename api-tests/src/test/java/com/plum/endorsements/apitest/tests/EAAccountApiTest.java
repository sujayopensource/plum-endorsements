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
import static org.hamcrest.Matchers.equalTo;

@Epic("Endorsement API")
@Feature("EA Accounts")
@DisplayName("GET /api/v1/ea-accounts")
class EAAccountApiTest extends BaseApiTest {

    @Test
    @DisplayName("Should return EA account when it exists")
    @Description("GET with valid employerId and insurerId returns account details")
    void shouldReturnEAAccount_WhenExists() {
        seedEAAccount(EMPLOYER_ID, INSURER_ID, new BigDecimal("50000.00"));

        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("insurerId", INSURER_ID)
                .when()
                .get("/api/v1/ea-accounts")
                .then()
                .statusCode(200)
                .body("employerId", equalTo(EMPLOYER_ID.toString()))
                .body("insurerId", equalTo(INSURER_ID.toString()))
                .body("balance", equalTo(50000.00f))
                .body("reserved", equalTo(0.0f))
                .body("availableBalance", equalTo(50000.00f));
    }

    @Test
    @DisplayName("Should return 404 when EA account not found")
    @Description("GET with non-existent employer/insurer pair returns 404")
    void shouldReturn404_WhenEAAccountNotFound() {
        given()
                .queryParam("employerId", UUID.randomUUID())
                .queryParam("insurerId", UUID.randomUUID())
                .when()
                .get("/api/v1/ea-accounts")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Should reflect reservation after ADD endorsement creation")
    @Description("Creating an ADD endorsement reserves funds, visible in the EA account")
    void shouldReflectReservation_AfterEndorsementCreation() {
        seedEAAccount(EMPLOYER_ID, INSURER_ID, new BigDecimal("10000.00"));

        Map<String, Object> request = createEndorsementRequest("ADD", new BigDecimal("2500.00"));
        createEndorsementViaApi(request).statusCode(201);

        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("insurerId", INSURER_ID)
                .when()
                .get("/api/v1/ea-accounts")
                .then()
                .statusCode(200)
                .body("balance", equalTo(10000.00f))
                .body("reserved", equalTo(2500.00f))
                .body("availableBalance", equalTo(7500.00f));
    }

    @Test
    @DisplayName("Should not change balance for DELETE endorsement")
    @Description("Creating a DELETE endorsement does not affect EA account balance")
    void shouldNotChangeBalance_ForDeleteEndorsement() {
        seedEAAccount(EMPLOYER_ID, INSURER_ID, new BigDecimal("10000.00"));

        Map<String, Object> request = createEndorsementRequest(
                EMPLOYER_ID, UUID.randomUUID(), INSURER_ID, POLICY_ID,
                "DELETE", LocalDate.of(2026, 4, 1), null,
                new BigDecimal("500.00"), null);
        createEndorsementViaApi(request).statusCode(201);

        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("insurerId", INSURER_ID)
                .when()
                .get("/api/v1/ea-accounts")
                .then()
                .statusCode(200)
                .body("balance", equalTo(10000.00f))
                .body("reserved", equalTo(0.0f))
                .body("availableBalance", equalTo(10000.00f));
    }
}
