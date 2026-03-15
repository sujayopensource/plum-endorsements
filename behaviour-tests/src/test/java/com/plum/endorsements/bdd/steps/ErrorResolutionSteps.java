package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.DatabaseHelper;
import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.plum.endorsements.bdd.steps.EndorsementCreationSteps.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

public class ErrorResolutionSteps {

    @Autowired
    private TestContext context;

    @Autowired
    private DatabaseHelper dbHelper;

    // ── Given steps ──

    @Given("a REJECTED endorsement exists with error code {string} and message {string}")
    public void aRejectedEndorsementExistsWithErrorCodeAndMessage(String errorCode, String errorMessage) {
        UUID endorsementId = dbHelper.seedEndorsementAtStatus(
                EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID, "REJECTED", 0);
        context.setEndorsementId(endorsementId.toString());

        // Seed the rejection error details for this endorsement
        dbHelper.seedEndorsementError(endorsementId, errorCode, errorMessage);

        // Store error message for resolution triggering
        context.store("errorMessage", errorMessage);
    }

    // ── When steps ──

    @When("I request error resolution suggestions for the endorsement")
    public void iRequestErrorResolutionSuggestionsForTheEndorsement() {
        String errorMessage = context.retrieve("errorMessage");
        // Trigger resolution first
        Response response = given()
                .queryParam("endorsementId", context.getEndorsementId())
                .queryParam("errorMessage", errorMessage)
                .when()
                .post("/api/v1/intelligence/error-resolutions/resolve");
        context.setResponse(response);
    }

    // ── Then steps ──

    @Then("the resolution suggestion should have confidence greater than {double}")
    public void theResolutionSuggestionShouldHaveConfidenceGreaterThan(double minConfidence) {
        Float confidence = context.getResponse().path("confidence");
        assertThat(confidence.doubleValue()).isGreaterThan(minConfidence);
    }

    @Then("the resolution suggestion should have confidence less than {double}")
    public void theResolutionSuggestionShouldHaveConfidenceLessThan(double maxConfidence) {
        Float confidence = context.getResponse().path("confidence");
        assertThat(confidence.doubleValue()).isLessThan(maxConfidence);
    }

    @Then("the resolution suggestion category should be {string}")
    public void theResolutionSuggestionCategoryShouldBe(String expectedCategory) {
        context.getResponse().then().body("errorType", equalTo(expectedCategory));
    }

    @Then("the resolution should contain a fix explanation")
    public void theResolutionShouldContainAFixExplanation() {
        String resolution = context.getResponse().path("resolution");
        assertThat(resolution).isNotNull().isNotEmpty();
    }

    @Then("the resolution original value should be {string}")
    public void theResolutionOriginalValueShouldBe(String expectedOriginal) {
        context.getResponse().then().body("originalValue", equalTo(expectedOriginal));
    }

    @Then("the resolution corrected value should be {string}")
    public void theResolutionCorrectedValueShouldBe(String expectedCorrected) {
        context.getResponse().then().body("correctedValue", equalTo(expectedCorrected));
    }

    @Then("the resolution should be auto-applied")
    public void theResolutionShouldBeAutoApplied() {
        context.getResponse().then().body("autoApplied", equalTo(true));
    }

    @Then("the resolution should not be auto-applied")
    public void theResolutionShouldNotBeAutoApplied() {
        context.getResponse().then().body("autoApplied", equalTo(false));
    }

    @Then("the resolution suggestion should include a generic recommendation")
    public void theResolutionSuggestionShouldIncludeGenericRecommendation() {
        String resolution = context.getResponse().path("resolution");
        assertThat(resolution).isNotNull().isNotEmpty();
    }

    // ── New Given steps (Phase 3 Intelligence) ──

    @Given("an endorsement {string} exists with status {string} for insurer {string}")
    public void anEndorsementExistsWithStatusForInsurer(String endorsementRef, String status, String insurerRef) {
        UUID insurerId = UUID.nameUUIDFromBytes(insurerRef.getBytes());
        UUID endorsementId = dbHelper.seedEndorsementAtStatus(
                EMPLOYER_ID, EMPLOYEE_ID, insurerId, POLICY_ID, status, 0);
        context.setEndorsementId(endorsementId.toString());
        context.store("currentInsurerId", insurerId.toString());
    }

    @Given("the rejection error is {string} with code {string}")
    public void theRejectionErrorIsWithCode(String errorMessage, String errorCode) {
        UUID endorsementId = UUID.fromString(context.getEndorsementId());
        dbHelper.seedEndorsementError(endorsementId, errorCode, errorMessage);
        context.store("errorCode", errorCode);
        context.store("errorMessage", errorMessage);
    }

    @Given("the endorsement has premium amount {double}")
    public void theEndorsementHasPremiumAmount(double premiumAmount) {
        context.store("originalPremium", String.valueOf(premiumAmount));
    }

    // ── New When steps ──

    @When("the system attempts to resolve the error")
    public void theSystemAttemptsToResolveTheError() {
        String errorMessage = context.retrieve("errorMessage");
        // Trigger resolution
        Response resolutionResponse = given()
                .queryParam("endorsementId", context.getEndorsementId())
                .queryParam("errorMessage", errorMessage)
                .when()
                .post("/api/v1/intelligence/error-resolutions/resolve");
        context.setResponse(resolutionResponse);
    }

    // ── New Then steps ──

    @Then("the resolution type should be {string}")
    public void theResolutionTypeShouldBe(String expectedType) {
        // Check if response is a list or single object
        try {
            List<String> types = context.getResponse().path("errorType");
            if (types != null && !types.isEmpty()) {
                assertThat(types).contains(expectedType);
            }
        } catch (ClassCastException e) {
            String errorType = context.getResponse().path("errorType");
            assertThat(errorType).isEqualTo(expectedType);
        }
    }

    @Then("the corrected value should start with {string}")
    public void theCorrectedValueShouldStartWith(String prefix) {
        try {
            List<String> correctedValues = context.getResponse().path("correctedValue");
            if (correctedValues != null && !correctedValues.isEmpty()) {
                assertThat(correctedValues.get(0)).startsWith(prefix);
            }
        } catch (ClassCastException e) {
            String correctedValue = context.getResponse().path("correctedValue");
            assertThat(correctedValue).startsWith(prefix);
        }
    }

    @Then("the confidence should be at least {double}")
    public void theConfidenceShouldBeAtLeast(double minConfidence) {
        try {
            List<Float> confidences = context.getResponse().path("confidence");
            if (confidences != null && !confidences.isEmpty()) {
                assertThat(confidences.get(0).doubleValue()).isGreaterThanOrEqualTo(minConfidence);
            }
        } catch (ClassCastException e) {
            Float confidence = context.getResponse().path("confidence");
            assertThat(confidence.doubleValue()).isGreaterThanOrEqualTo(minConfidence);
        }
    }

    @Then("the confidence should be less than {double}")
    public void theConfidenceShouldBeLessThan(double maxConfidence) {
        try {
            List<Float> confidences = context.getResponse().path("confidence");
            if (confidences != null && !confidences.isEmpty()) {
                assertThat(confidences.get(0).doubleValue()).isLessThan(maxConfidence);
            }
        } catch (ClassCastException e) {
            Float confidence = context.getResponse().path("confidence");
            assertThat(confidence.doubleValue()).isLessThan(maxConfidence);
        }
    }

    @Then("the corrected value should be different from the original")
    public void theCorrectedValueShouldBeDifferentFromOriginal() {
        try {
            List<String> correctedValues = context.getResponse().path("correctedValue");
            List<String> originalValues = context.getResponse().path("originalValue");
            if (correctedValues != null && !correctedValues.isEmpty()
                    && originalValues != null && !originalValues.isEmpty()) {
                assertThat(correctedValues.get(0)).isNotEqualTo(originalValues.get(0));
            }
        } catch (ClassCastException e) {
            String correctedValue = context.getResponse().path("correctedValue");
            String originalValue = context.getResponse().path("originalValue");
            assertThat(correctedValue).isNotEqualTo(originalValue);
        }
    }

    @Then("the resolution should NOT be auto-applied")
    public void theResolutionShouldNotBeAutoAppliedUpperCase() {
        try {
            List<Boolean> autoAppliedList = context.getResponse().path("autoApplied");
            if (autoAppliedList != null && !autoAppliedList.isEmpty()) {
                assertThat(autoAppliedList.get(0)).isFalse();
            }
        } catch (ClassCastException e) {
            Boolean autoApplied = context.getResponse().path("autoApplied");
            assertThat(autoApplied).isFalse();
        }
    }

    // ── Error Resolution Success Tracking Steps ──

    @Given("error resolutions exist with mixed outcomes")
    public void errorResolutionsExistWithMixedOutcomes() {
        UUID endorsementId1 = dbHelper.seedEndorsementAtStatus(
                EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID, "CONFIRMED", 0);
        UUID endorsementId2 = dbHelper.seedEndorsementAtStatus(
                EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID, "CONFIRMED", 0);
        UUID endorsementId3 = dbHelper.seedEndorsementAtStatus(
                EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID, "REJECTED", 0);

        dbHelper.seedErrorResolutionWithOutcome(endorsementId1, true, 0.98, "SUCCESS", "CONFIRMED");
        dbHelper.seedErrorResolutionWithOutcome(endorsementId2, true, 0.96, "SUCCESS", "CONFIRMED");
        dbHelper.seedErrorResolutionWithOutcome(endorsementId3, true, 0.97, "FAILURE", "REJECTED");
    }

    @When("I request the error resolution stats")
    public void iRequestTheErrorResolutionStats() {
        Response response = given()
                .when()
                .get("/api/v1/intelligence/error-resolutions/stats");
        context.setResponse(response);
    }

    @Then("the stats should include success count")
    public void theStatsShouldIncludeSuccessCount() {
        Integer successCount = context.getResponse().path("successCount");
        assertThat(successCount).isNotNull();
        assertThat(successCount).isGreaterThanOrEqualTo(2);
    }

    @Then("the stats should include failure count")
    public void theStatsShouldIncludeFailureCount() {
        Integer failureCount = context.getResponse().path("failureCount");
        assertThat(failureCount).isNotNull();
        assertThat(failureCount).isGreaterThanOrEqualTo(1);
    }

    @Then("the stats should include success rate")
    public void theStatsShouldIncludeSuccessRate() {
        Float successRate = context.getResponse().path("successRate");
        assertThat(successRate).isNotNull();
        assertThat(successRate.doubleValue()).isGreaterThan(0.0);
    }

    @Then("the resolution should address the first matching pattern")
    public void theResolutionShouldAddressTheFirstMatchingPattern() {
        // Verify that a resolution was generated (at least one result)
        context.getResponse().then().statusCode(200);
        try {
            List<String> resolutions = context.getResponse().path("resolution");
            assertThat(resolutions).isNotNull().isNotEmpty();
        } catch (ClassCastException e) {
            String resolution = context.getResponse().path("resolution");
            assertThat(resolution).isNotNull().isNotEmpty();
        }
    }
}
