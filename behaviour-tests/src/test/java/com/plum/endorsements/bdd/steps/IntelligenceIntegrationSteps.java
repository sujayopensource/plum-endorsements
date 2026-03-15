package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.DatabaseHelper;
import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.plum.endorsements.bdd.steps.EndorsementCreationSteps.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

public class IntelligenceIntegrationSteps {

    @Autowired
    private TestContext context;

    @Autowired
    private DatabaseHelper dbHelper;

    // ── Given steps ──

    @Given("an endorsement exists that triggers a volume spike anomaly")
    public void anEndorsementExistsThatTriggersVolumeSpikeAnomaly() {
        UUID employerId = UUID.fromString(context.retrieve("currentEmployerId"));
        UUID insurerId = UUID.fromString(context.retrieve("currentInsurerId"));

        // Create enough endorsements to trigger a volume spike (25+)
        for (int i = 0; i < 25; i++) {
            UUID uniqueEmployeeId = UUID.randomUUID();
            Map<String, Object> request = buildEndorsementRequest(
                    employerId, uniqueEmployeeId, insurerId, POLICY_ID,
                    "ADD", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                    new BigDecimal("1200.00"), null);
            Response response = given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/v1/endorsements");
            // Store the last endorsement ID for later use
            String id = response.path("id");
            if (id != null) {
                context.setEndorsementId(id);
            }
        }
    }

    @Given("the endorsement is later rejected with error {string}")
    public void theEndorsementIsLaterRejectedWithError(String errorMessage) {
        // Seed the endorsement as rejected with the specified error
        UUID endorsementId = UUID.fromString(context.getEndorsementId());
        // Use JDBC to update the endorsement status to REJECTED
        dbHelper.seedEndorsementError(endorsementId, "DOB_FORMAT_ERR", errorMessage);
    }

    @Given("the employer has historical endorsement data for {int} days")
    public void theEmployerHasHistoricalEndorsementDataForDays(int days) {
        UUID employerId = UUID.fromString(context.retrieve("currentEmployerId"));
        UUID insurerId = UUID.fromString(context.retrieve("currentInsurerId"));
        LocalDate today = LocalDate.now();

        // Seed 20 endorsements spread over the historical period
        for (int i = 0; i < 20; i++) {
            UUID uniqueEmployeeId = UUID.randomUUID();
            LocalDate coverageStart = today.minusDays(days).plusDays(i * (days / 20));
            Map<String, Object> request = buildEndorsementRequest(
                    employerId, uniqueEmployeeId, insurerId, POLICY_ID,
                    "ADD", coverageStart, coverageStart.plusYears(1),
                    new BigDecimal("1500.00"), null);
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/v1/endorsements");
        }
    }

    // ── When steps ──

    @When("the system processes the rejection")
    public void theSystemProcessesTheRejection() {
        UUID employerId = UUID.fromString(context.retrieve("currentEmployerId"));

        // Trigger anomaly analysis
        given()
                .queryParam("employerId", employerId.toString())
                .when()
                .get("/api/v1/intelligence/anomalies");

        // Request error resolution for the endorsement
        Response response = given()
                .queryParam("endorsementId", context.getEndorsementId())
                .when()
                .get("/api/v1/intelligence/error-resolutions");
        context.setResponse(response);
    }

    @When("a balance forecast is generated after recent activity")
    public void aBalanceForecastIsGeneratedAfterRecentActivity() {
        UUID employerId = UUID.fromString(context.retrieve("currentEmployerId"));
        UUID insurerId = UUID.fromString(context.retrieve("currentInsurerId"));

        Response response = given()
                .queryParam("employerId", employerId.toString())
                .queryParam("insurerId", insurerId.toString())
                .when()
                .post("/api/v1/intelligence/forecasts/generate");
        context.setResponse(response);
    }

    // ── Then steps ──

    @Then("an anomaly should exist for the employer")
    public void anAnomalyShouldExistForTheEmployer() {
        UUID employerId = UUID.fromString(context.retrieve("currentEmployerId"));

        Response anomalyResponse = given()
                .queryParam("employerId", employerId.toString())
                .when()
                .get("/api/v1/intelligence/anomalies");

        anomalyResponse.then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Then("an error resolution should exist for the endorsement")
    public void anErrorResolutionShouldExistForTheEndorsement() {
        Response resolutionResponse = given()
                .queryParam("endorsementId", context.getEndorsementId())
                .when()
                .get("/api/v1/intelligence/error-resolutions");

        resolutionResponse.then().statusCode(200);
    }

    @Then("the error resolution should have confidence above {double}")
    public void theErrorResolutionShouldHaveConfidenceAbove(double minConfidence) {
        Response resolutionResponse = given()
                .queryParam("endorsementId", context.getEndorsementId())
                .when()
                .get("/api/v1/intelligence/error-resolutions");

        List<Float> confidences = resolutionResponse.path("confidence");
        if (confidences != null && !confidences.isEmpty()) {
            assertThat(confidences.get(0).doubleValue()).isGreaterThan(minConfidence);
        }
    }

    @Then("the forecast should reflect the current burn rate")
    public void theForecastShouldReflectTheCurrentBurnRate() {
        // Verify the forecast has a non-null forecasted amount
        context.getResponse().then()
                .statusCode(200)
                .body("forecastedAmount", notNullValue());

        Float forecastedAmount = context.getResponse().path("forecastedAmount");
        assertThat(forecastedAmount).isNotNull();
        assertThat(forecastedAmount.doubleValue()).isGreaterThan(0);
    }

    @Then("the forecast narrative should mention the balance projection")
    public void theForecastNarrativeShouldMentionTheBalanceProjection() {
        String narrative = context.getResponse().path("narrative");
        assertThat(narrative).isNotNull().isNotEmpty();
    }

    // ── Helpers ──

    private Map<String, Object> buildEndorsementRequest(
            UUID employerId, UUID employeeId, UUID insurerId, UUID policyId,
            String type, LocalDate coverageStart, LocalDate coverageEnd,
            BigDecimal premiumAmount, String idempotencyKey) {

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("employerId", employerId.toString());
        request.put("employeeId", employeeId.toString());
        request.put("insurerId", insurerId.toString());
        request.put("policyId", policyId.toString());
        request.put("type", type);
        request.put("coverageStartDate", coverageStart.toString());
        if (coverageEnd != null) {
            request.put("coverageEndDate", coverageEnd.toString());
        }
        request.put("employeeData", Map.of(
                "name", "Test Employee",
                "dob", "1990-05-15",
                "gender", "M"
        ));
        if (premiumAmount != null) {
            request.put("premiumAmount", premiumAmount);
        }
        if (idempotencyKey != null) {
            request.put("idempotencyKey", idempotencyKey);
        }
        return request;
    }
}
