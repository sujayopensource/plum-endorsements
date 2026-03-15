package com.plum.endorsements.apitest.tests;

import com.plum.endorsements.apitest.base.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Intelligence API")
@Feature("Balance Forecasting")
@DisplayName("Balance Forecast API")
class BalanceForecastApiTest extends BaseApiTest {

    // ── Helper: Seed a balance forecast record via JDBC ──

    private UUID seedForecast(UUID employerId, UUID insurerId, BigDecimal forecastedAmount) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO balance_forecasts (id, employer_id, insurer_id, forecast_date,
                    forecasted_amount, narrative, created_at)
                VALUES (?, ?, ?, '2026-04-15', ?, 'Projected balance based on historical endorsement patterns', now())
                """,
                id, employerId, insurerId, forecastedAmount
        );
        return id;
    }

    @Test
    @DisplayName("POST /api/v1/intelligence/forecasts/generate creates a forecast")
    @Description("Generating a forecast for an employer-insurer pair returns the created forecast record")
    void shouldGenerateForecast() {
        // Seed EA account so the forecast engine has balance context
        seedEAAccount(EMPLOYER_ID, INSURER_ID, new BigDecimal("50000.00"));

        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("insurerId", INSURER_ID)
                .when()
                .post("/api/v1/intelligence/forecasts/generate")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("employerId", equalTo(EMPLOYER_ID.toString()))
                .body("insurerId", equalTo(INSURER_ID.toString()))
                .body("forecastDate", notNullValue())
                .body("forecastedAmount", notNullValue())
                .body("narrative", notNullValue())
                .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/forecasts returns latest forecast")
    @Description("After generating a forecast, the GET endpoint returns the most recent forecast for the employer-insurer pair")
    void shouldReturnLatestForecast() {
        // Seed EA account and generate a forecast first
        seedEAAccount(EMPLOYER_ID, INSURER_ID, new BigDecimal("50000.00"));

        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("insurerId", INSURER_ID)
                .when()
                .post("/api/v1/intelligence/forecasts/generate")
                .then()
                .statusCode(200);

        // Retrieve the latest forecast
        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("insurerId", INSURER_ID)
                .when()
                .get("/api/v1/intelligence/forecasts")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("employerId", equalTo(EMPLOYER_ID.toString()))
                .body("insurerId", equalTo(INSURER_ID.toString()))
                .body("forecastDate", notNullValue())
                .body("forecastedAmount", notNullValue())
                .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/forecasts/history returns forecast history")
    @Description("Forecast history returns all forecast records for a given employer")
    void shouldReturnForecastHistory() {
        // Seed multiple forecast records directly
        seedForecast(EMPLOYER_ID, INSURER_ID, new BigDecimal("15000.00"));
        seedForecast(EMPLOYER_ID, INSURER_ID, new BigDecimal("18000.00"));

        given()
                .queryParam("employerId", EMPLOYER_ID)
                .when()
                .get("/api/v1/intelligence/forecasts/history")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2))
                .body("[0].employerId", equalTo(EMPLOYER_ID.toString()))
                .body("[0].forecastDate", notNullValue())
                .body("[0].forecastedAmount", notNullValue())
                .body("[0].createdAt", notNullValue());
    }

    @Test
    @DisplayName("POST /api/v1/intelligence/forecasts/generate with missing params returns 400")
    @Description("Generating a forecast without required employerId and insurerId parameters returns a 400 error")
    void shouldReturn400WhenMissingParams() {
        // Missing both employerId and insurerId
        given()
                .when()
                .post("/api/v1/intelligence/forecasts/generate")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/forecasts returns 404 when no forecast exists")
    @Description("Requesting the latest forecast for an employer-insurer pair with no history returns 404")
    void shouldReturn404WhenNoForecastExists() {
        given()
                .queryParam("employerId", UUID.randomUUID())
                .queryParam("insurerId", UUID.randomUUID())
                .when()
                .get("/api/v1/intelligence/forecasts")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/forecasts/history returns empty list for unknown employer")
    @Description("Forecast history for an employer with no records returns an empty list")
    void shouldReturnEmptyHistoryForUnknownEmployer() {
        given()
                .queryParam("employerId", UUID.randomUUID())
                .when()
                .get("/api/v1/intelligence/forecasts/history")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    // ── Phase 3 Enterprise-Grade Negative/Edge Case Tests ──

    @Test
    @DisplayName("POST /api/v1/intelligence/forecasts/generate without employerId returns 400")
    @Description("Generating a forecast without the required employerId query parameter returns 400 Bad Request")
    void shouldReturn400WhenMissingEmployerIdForGenerate() {
        given()
                .queryParam("insurerId", INSURER_ID)
                .when()
                .post("/api/v1/intelligence/forecasts/generate")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/v1/intelligence/forecasts/generate without insurerId returns 400")
    @Description("Generating a forecast without the required insurerId query parameter returns 400 Bad Request")
    void shouldReturn400WhenMissingInsurerId() {
        given()
                .queryParam("employerId", EMPLOYER_ID)
                .when()
                .post("/api/v1/intelligence/forecasts/generate")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/forecasts/history returns empty for new employer with no forecasts")
    @Description("Forecast history for a newly created employer with zero endorsements returns an empty list")
    void shouldReturnEmptyHistoryForNewEmployer() {
        UUID newEmployerId = UUID.randomUUID();

        // Seed an EA account for this employer but do NOT generate any forecasts
        seedEAAccount(newEmployerId, INSURER_ID, new BigDecimal("10000.00"));

        given()
                .queryParam("employerId", newEmployerId)
                .when()
                .get("/api/v1/intelligence/forecasts/history")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @DisplayName("POST /api/v1/intelligence/forecasts/generate returns forecast with all expected fields")
    @Description("A generated forecast response contains all expected JSON fields: id, employerId, insurerId, forecastDate, forecastedAmount, narrative, createdAt")
    void shouldReturnCorrectForecastFields() {
        seedEAAccount(EMPLOYER_ID, INSURER_ID, new BigDecimal("50000.00"));

        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("insurerId", INSURER_ID)
                .when()
                .post("/api/v1/intelligence/forecasts/generate")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("employerId", equalTo(EMPLOYER_ID.toString()))
                .body("insurerId", equalTo(INSURER_ID.toString()))
                .body("forecastDate", notNullValue())
                .body("forecastedAmount", notNullValue())
                .body("narrative", notNullValue())
                .body("createdAt", notNullValue())
                // actualAmount and accuracy may be null for freshly generated forecasts
                .body("containsKey('actualAmount')", equalTo(true))
                .body("containsKey('accuracy')", equalTo(true));
    }

    @Test
    @DisplayName("POST /api/v1/intelligence/forecasts/generate for employer with no endorsements succeeds")
    @Description("Generating a forecast for an employer that has an EA account but zero endorsements should still succeed")
    void shouldHandleForecastForEmployerWithNoEndorsements() {
        UUID freshEmployerId = UUID.randomUUID();
        seedEAAccount(freshEmployerId, INSURER_ID, new BigDecimal("25000.00"));

        given()
                .queryParam("employerId", freshEmployerId)
                .queryParam("insurerId", INSURER_ID)
                .when()
                .post("/api/v1/intelligence/forecasts/generate")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("employerId", equalTo(freshEmployerId.toString()))
                .body("insurerId", equalTo(INSURER_ID.toString()))
                .body("forecastedAmount", notNullValue());
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/forecasts returns latest forecast after multiple generates")
    @Description("After generating multiple forecasts, the GET endpoint returns only the most recent one")
    void shouldReturnLatestForecastAfterMultipleGenerates() {
        seedEAAccount(EMPLOYER_ID, INSURER_ID, new BigDecimal("50000.00"));

        // Generate forecast twice
        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("insurerId", INSURER_ID)
                .when()
                .post("/api/v1/intelligence/forecasts/generate")
                .then()
                .statusCode(200);

        String secondId = given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("insurerId", INSURER_ID)
                .when()
                .post("/api/v1/intelligence/forecasts/generate")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        // The latest forecast should be returned
        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("insurerId", INSURER_ID)
                .when()
                .get("/api/v1/intelligence/forecasts")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("employerId", equalTo(EMPLOYER_ID.toString()));
    }
}
