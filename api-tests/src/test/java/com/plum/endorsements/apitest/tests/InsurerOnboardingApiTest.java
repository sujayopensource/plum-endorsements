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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Multi-Insurer API")
@Feature("Self-Service Insurer Onboarding")
@DisplayName("POST/PUT /api/v1/insurers")
class InsurerOnboardingApiTest extends BaseApiTest {

    @Test
    @DisplayName("Should create a new insurer configuration")
    @Description("POST /api/v1/insurers creates a new insurer and returns 201 with location header")
    void shouldCreateNewInsurerConfiguration() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "insurerName", "Test Insurer API",
                        "insurerCode", "TEST_API",
                        "adapterType", "MOCK",
                        "supportsRealTime", true,
                        "supportsBatch", false,
                        "maxBatchSize", 50,
                        "batchSlaHours", 12,
                        "rateLimitPerMinute", 30,
                        "dataFormat", "JSON"
                ))
                .when()
                .post("/api/v1/insurers")
                .then()
                .statusCode(201)
                .header("Location", containsString("/api/v1/insurers/"))
                .body("insurerName", equalTo("Test Insurer API"))
                .body("insurerCode", equalTo("TEST_API"))
                .body("adapterType", equalTo("MOCK"))
                .body("supportsRealTime", equalTo(true))
                .body("supportsBatch", equalTo(false))
                .body("active", equalTo(true));
    }

    @Test
    @DisplayName("Should update an existing insurer configuration")
    @Description("PUT /api/v1/insurers/{id} updates specific fields of an insurer")
    void shouldUpdateInsurerConfiguration() {
        // Use the MOCK insurer
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "insurerName", "Updated Mock Insurer",
                        "rateLimitPerMinute", 100
                ))
                .when()
                .put("/api/v1/insurers/{id}", INSURER_ID)
                .then()
                .statusCode(200)
                .body("insurerName", equalTo("Updated Mock Insurer"))
                .body("rateLimitPerMinute", equalTo(100));
    }

    @Test
    @DisplayName("Should deactivate an insurer via update")
    @Description("PUT /api/v1/insurers/{id} with active=false deactivates the insurer")
    void shouldDeactivateInsurer() {
        // First create a new insurer to deactivate (don't deactivate the existing ones)
        String insurerId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "insurerName", "Deactivate Test",
                        "insurerCode", "DEACT",
                        "adapterType", "MOCK",
                        "supportsRealTime", true,
                        "supportsBatch", false,
                        "maxBatchSize", 50,
                        "batchSlaHours", 24,
                        "rateLimitPerMinute", 30
                ))
                .when()
                .post("/api/v1/insurers")
                .then()
                .statusCode(201)
                .extract().path("insurerId");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("active", false))
                .when()
                .put("/api/v1/insurers/{id}", insurerId)
                .then()
                .statusCode(200)
                .body("active", equalTo(false));
    }

    @Test
    @DisplayName("Should return 400 for invalid create request")
    @Description("POST /api/v1/insurers without required fields returns validation error")
    void shouldReturn400ForInvalidRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "insurerName", ""  // blank name
                ))
                .when()
                .post("/api/v1/insurers")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent insurer")
    @Description("PUT /api/v1/insurers/{id} with unknown ID returns 404")
    void shouldReturn404ForUnknownInsurer() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("insurerName", "Ghost"))
                .when()
                .put("/api/v1/insurers/{id}", UUID.randomUUID())
                .then()
                .statusCode(404);
    }
}
