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
@Feature("Insurer Configuration")
@DisplayName("GET /api/v1/insurers")
class InsurerConfigurationApiTest extends BaseApiTest {

    @Test
    @DisplayName("Should list all active insurers")
    @Description("GET /api/v1/insurers returns at least 4 active insurers (Mock, ICICI, Niva, Bajaj) with required fields")
    void shouldListAllActiveInsurers() {
        given()
                .when()
                .get("/api/v1/insurers")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(4))
                .body("insurerName", everyItem(notNullValue()))
                .body("insurerCode", everyItem(notNullValue()))
                .body("adapterType", everyItem(notNullValue()))
                .body("active", everyItem(equalTo(true)));
    }

    @Test
    @DisplayName("Should get insurer by ID")
    @Description("GET /api/v1/insurers/{id} returns the Mock insurer with correct code and adapter type")
    void shouldGetInsurerById() {
        given()
                .when()
                .get("/api/v1/insurers/{id}", INSURER_ID)
                .then()
                .statusCode(200)
                .body("insurerId", equalTo(INSURER_ID.toString()))
                .body("insurerCode", equalTo("MOCK"))
                .body("adapterType", equalTo("MOCK"))
                .body("active", equalTo(true));
    }

    @Test
    @DisplayName("Should get insurer capabilities")
    @Description("GET /api/v1/insurers/{id}/capabilities returns capability flags for ICICI Lombard (real-time only)")
    void shouldGetInsurerCapabilities() {
        given()
                .when()
                .get("/api/v1/insurers/{id}/capabilities", ICICI_INSURER_ID)
                .then()
                .statusCode(200)
                .body("supportsRealTime", equalTo(true))
                .body("supportsBatch", equalTo(false));
    }

    @Test
    @DisplayName("Should return 404 for unknown insurer")
    @Description("GET /api/v1/insurers/{id} with a random UUID returns 404")
    void shouldReturn404ForUnknownInsurer() {
        given()
                .when()
                .get("/api/v1/insurers/{id}", UUID.randomUUID())
                .then()
                .statusCode(404);
    }
}
