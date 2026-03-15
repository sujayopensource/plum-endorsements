package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.DatabaseHelper;
import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.plum.endorsements.bdd.steps.EndorsementCreationSteps.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

public class ProcessMiningSteps {

    private static final UUID MOCK_INSURER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private TestContext context;

    @Autowired
    private DatabaseHelper dbHelper;

    // ── Given steps ──

    @Given("endorsements exist with the following lifecycle times for the mock insurer:")
    public void endorsementsExistWithLifecycleTimesForMockInsurer(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            String fromStatus = row.get("fromStatus");
            String toStatus = row.get("toStatus");
            int avgDurationMinutes = Integer.parseInt(row.get("avgDurationMinutes"));

            // Seed endorsements with lifecycle event transitions and timestamps
            for (int i = 0; i < 5; i++) {
                UUID endorsementId = dbHelper.seedEndorsementAtStatus(
                        EMPLOYER_ID, UUID.randomUUID(), MOCK_INSURER_ID, POLICY_ID,
                        toStatus, 0);
                dbHelper.seedLifecycleTransition(endorsementId, fromStatus, toStatus, avgDurationMinutes);
            }
        }
    }

    @Given("{int} endorsements completed with status {string} for the mock insurer")
    public void endorsementsCompletedWithStatusForMockInsurer(int count, String status) {
        for (int i = 0; i < count; i++) {
            dbHelper.seedEndorsementAtStatus(
                    EMPLOYER_ID, UUID.randomUUID(), MOCK_INSURER_ID, POLICY_ID,
                    status, 0);
        }
    }

    @Given("{int} endorsements followed the happy path for the mock insurer")
    public void endorsementsFollowedHappyPathForMockInsurer(int count) {
        for (int i = 0; i < count; i++) {
            UUID endorsementId = dbHelper.seedEndorsementAtStatus(
                    EMPLOYER_ID, UUID.randomUUID(), MOCK_INSURER_ID, POLICY_ID,
                    "CONFIRMED", 0);
            dbHelper.seedHappyPathLifecycle(endorsementId);
        }
    }

    @Given("{int} endorsements deviated from the happy path for the mock insurer")
    public void endorsementsDeviatedFromHappyPathForMockInsurer(int count) {
        for (int i = 0; i < count; i++) {
            UUID endorsementId = dbHelper.seedEndorsementAtStatus(
                    EMPLOYER_ID, UUID.randomUUID(), MOCK_INSURER_ID, POLICY_ID,
                    "CONFIRMED", 1);
            dbHelper.seedDeviatedLifecycle(endorsementId);
        }
    }

    // ── When steps ──

    @When("I request process mining analysis for the mock insurer")
    public void iRequestProcessMiningAnalysisForMockInsurer() {
        // First trigger analysis
        given()
                .when()
                .post("/api/v1/intelligence/process-mining/analyze");

        // Then fetch metrics
        Response response = given()
                .queryParam("insurerId", MOCK_INSURER_ID.toString())
                .when()
                .get("/api/v1/intelligence/process-mining/metrics");
        context.setResponse(response);
    }

    @When("I request the STP rate for the mock insurer")
    public void iRequestTheSTPRateForMockInsurer() {
        // First trigger analysis
        given()
                .when()
                .post("/api/v1/intelligence/process-mining/analyze");

        // Then fetch STP rate
        Response response = given()
                .queryParam("insurerId", MOCK_INSURER_ID.toString())
                .when()
                .get("/api/v1/intelligence/process-mining/stp-rate");
        context.setResponse(response);
    }

    // ── Then steps ──

    @Then("the bottleneck analysis should identify {string} to {string} as the slowest transition")
    public void theBottleneckAnalysisShouldIdentifyAsSlowestTransition(String fromStatus, String toStatus) {
        List<Map<String, Object>> metrics = context.getResponse().jsonPath().getList("$");
        assertThat(metrics).as("Metrics list should not be empty").isNotEmpty();

        // Find the metric with the highest avgDurationMs
        Map<String, Object> slowest = metrics.stream()
                .max((a, b) -> {
                    long aDur = ((Number) a.get("avgDurationMs")).longValue();
                    long bDur = ((Number) b.get("avgDurationMs")).longValue();
                    return Long.compare(aDur, bDur);
                })
                .orElseThrow(() -> new AssertionError("No metrics found"));

        assertThat(slowest.get("fromStatus").toString()).isEqualTo(fromStatus);
        assertThat(slowest.get("toStatus").toString()).isEqualTo(toStatus);
    }

    @Then("the bottleneck average duration should be greater than {int} minutes")
    public void theBottleneckAverageDurationShouldBeGreaterThanMinutes(int minMinutes) {
        List<Map<String, Object>> metrics = context.getResponse().jsonPath().getList("$");
        assertThat(metrics).as("Metrics list should not be empty").isNotEmpty();

        // Find the metric with the highest avgDurationMs and convert to minutes
        long maxDurationMs = metrics.stream()
                .mapToLong(m -> ((Number) m.get("avgDurationMs")).longValue())
                .max()
                .orElse(0);

        long durationMinutes = maxDurationMs / 60000;
        assertThat(durationMinutes).isGreaterThan(minMinutes);
    }

    @Then("the STP rate should be {double}")
    public void theStpRateShouldBe(double expectedRate) {
        // Trigger analysis and fetch STP rate for mock insurer
        given()
                .when()
                .post("/api/v1/intelligence/process-mining/analyze");

        Response stpResponse = given()
                .queryParam("insurerId", MOCK_INSURER_ID.toString())
                .when()
                .get("/api/v1/intelligence/process-mining/stp-rate");

        Number actualRate = stpResponse.path("overallStpRate");
        assertThat(actualRate).as("overallStpRate should not be null").isNotNull();
        assertThat(actualRate.doubleValue()).isCloseTo(expectedRate, org.assertj.core.data.Offset.offset(0.5));
    }

    @Then("the total endorsements processed should be {int}")
    public void theTotalEndorsementsProcessedShouldBe(int expectedTotal) {
        context.getResponse().then().body("totalProcessed", equalTo(expectedTotal));
    }

    @Then("the successful count should be {int}")
    public void theSuccessfulCountShouldBe(int expectedCount) {
        context.getResponse().then().body("successfulCount", equalTo(expectedCount));
    }

    @Then("the happy path percentage should be {double}")
    public void theHappyPathPercentageShouldBe(double expectedPercentage) {
        // Repurposed: reads overallStpRate from stp-rate endpoint
        given()
                .when()
                .post("/api/v1/intelligence/process-mining/analyze");

        Response stpResponse = given()
                .queryParam("insurerId", MOCK_INSURER_ID.toString())
                .when()
                .get("/api/v1/intelligence/process-mining/stp-rate");

        Number actualRate = stpResponse.path("overallStpRate");
        assertThat(actualRate).as("overallStpRate should not be null").isNotNull();
        assertThat(actualRate.doubleValue()).isCloseTo(expectedPercentage, org.assertj.core.data.Offset.offset(0.5));
    }

    @Then("the analysis should contain field {string}")
    public void theAnalysisShouldContainField(String field) {
        context.getResponse().then().body(field, notNullValue());
    }

    @Given("STP rate snapshots exist for the mock insurer over the last {int} days")
    public void stpRateSnapshotsExistForMockInsurerOverLastDays(int days) {
        for (int i = days; i >= 1; i--) {
            int total = 10 + i;
            int stp = 8 + i;
            double rate = (double) stp / total * 100;
            dbHelper.seedStpRateSnapshot(MOCK_INSURER_ID, i, total, stp, rate);
        }
    }

    @When("I request the STP rate trend for the mock insurer")
    public void iRequestTheStpRateTrendForMockInsurer() {
        Response response = given()
                .queryParam("insurerId", MOCK_INSURER_ID.toString())
                .queryParam("days", 30)
                .when()
                .get("/api/v1/intelligence/process-mining/stp-rate/trend");
        context.setResponse(response);
    }

    @Then("the trend should contain at least {int} data points")
    public void theTrendShouldContainAtLeastDataPoints(int minCount) {
        context.getResponse().then().body("dataPoints.size()", greaterThanOrEqualTo(minCount));
    }

    @Then("the trend current rate should be greater than {int}")
    public void theTrendCurrentRateShouldBeGreaterThan(int minRate) {
        Number currentRate = context.getResponse().path("currentRate");
        assertThat(currentRate.doubleValue()).isGreaterThan(minRate);
    }

    // ── New Given steps (Phase 3 Intelligence) ──

    @Given("an insurer {string} has processed endorsements")
    public void anInsurerHasProcessedEndorsements(String insurerRef) {
        UUID insurerId = UUID.nameUUIDFromBytes(insurerRef.getBytes());
        context.store("testInsurerId", insurerId.toString());
        // Ensure this custom insurer is registered in insurer_configurations
        dbHelper.seedInsurerConfiguration(insurerId, insurerRef, "CUSTOM_" + insurerRef.replace("-", "_"));
    }

    @Given("{int} endorsements followed the happy path")
    public void endorsementsFollowedTheHappyPath(int count) {
        UUID insurerId = UUID.fromString(context.retrieve("testInsurerId"));
        for (int i = 0; i < count; i++) {
            UUID endorsementId = dbHelper.seedEndorsementAtStatus(
                    EMPLOYER_ID, UUID.randomUUID(), insurerId, POLICY_ID,
                    "CONFIRMED", 0);
            dbHelper.seedHappyPathLifecycle(endorsementId);
        }
    }

    @Given("{int} endorsements had rejected-and-retried paths")
    public void endorsementsHadRejectedAndRetriedPaths(int count) {
        UUID insurerId = UUID.fromString(context.retrieve("testInsurerId"));
        for (int i = 0; i < count; i++) {
            UUID endorsementId = dbHelper.seedEndorsementAtStatus(
                    EMPLOYER_ID, UUID.randomUUID(), insurerId, POLICY_ID,
                    "CONFIRMED", 1);
            dbHelper.seedDeviatedLifecycle(endorsementId);
        }
    }

    @Given("insurer {string} has average lifecycle of {int} hours")
    public void insurerHasAverageLifecycleOfHours(String insurerRef, int hours) {
        UUID insurerId = UUID.nameUUIDFromBytes(insurerRef.getBytes());
        context.store("insurer-" + insurerRef, insurerId.toString());

        // Ensure this custom insurer is registered in insurer_configurations
        dbHelper.seedInsurerConfiguration(insurerId, insurerRef, "CUSTOM_" + insurerRef.replace("-", "_"));

        // Seed endorsements with lifecycle transitions matching the average duration
        for (int i = 0; i < 10; i++) {
            UUID endorsementId = dbHelper.seedEndorsementAtStatus(
                    EMPLOYER_ID, UUID.randomUUID(), insurerId, POLICY_ID,
                    "CONFIRMED", 0);
            // Distribute the total duration across 3 transitions
            int minutesPerTransition = (hours * 60) / 3;
            dbHelper.seedLifecycleTransition(endorsementId,
                    "PROVISIONALLY_COVERED", "QUEUED_FOR_BATCH", minutesPerTransition);
            dbHelper.seedLifecycleTransition(endorsementId,
                    "QUEUED_FOR_BATCH", "BATCH_SUBMITTED", minutesPerTransition);
            dbHelper.seedLifecycleTransition(endorsementId,
                    "BATCH_SUBMITTED", "CONFIRMED", minutesPerTransition);
        }
    }

    @Given("{int} insurers with different processing patterns")
    public void insurersWithDifferentProcessingPatterns(int count) {
        List<String> insurerIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID insurerId = UUID.nameUUIDFromBytes(("MULTI-INS-" + i).getBytes());
            insurerIds.add(insurerId.toString());
            // Ensure custom insurer is registered
            dbHelper.seedInsurerConfiguration(insurerId, "Multi Insurer " + i, "MULTI_INS_" + i);

            int happyCount = 6 + (i * 2); // 6, 8, 10
            int deviatedCount = 4 - i;     // 4, 3, 2

            for (int j = 0; j < happyCount; j++) {
                UUID endorsementId = dbHelper.seedEndorsementAtStatus(
                        EMPLOYER_ID, UUID.randomUUID(), insurerId, POLICY_ID,
                        "CONFIRMED", 0);
                dbHelper.seedHappyPathLifecycle(endorsementId);
            }
            for (int j = 0; j < deviatedCount; j++) {
                UUID endorsementId = dbHelper.seedEndorsementAtStatus(
                        EMPLOYER_ID, UUID.randomUUID(), insurerId, POLICY_ID,
                        "CONFIRMED", 1);
                dbHelper.seedDeviatedLifecycle(endorsementId);
            }
        }
        context.store("multiInsurerIds", String.join(",", insurerIds));
    }

    // ── New When steps ──

    @When("process mining analysis is triggered for insurer {string}")
    public void processMiningAnalysisIsTriggeredForInsurer(String insurerRef) {
        UUID insurerId = UUID.fromString(context.retrieve("testInsurerId"));

        // First trigger the analysis
        given()
                .when()
                .post("/api/v1/intelligence/process-mining/analyze");

        // Then fetch the STP rate
        Response response = given()
                .queryParam("insurerId", insurerId.toString())
                .when()
                .get("/api/v1/intelligence/process-mining/stp-rate");
        context.setResponse(response);
    }

    @When("process mining insights are generated")
    public void processMiningInsightsAreGenerated() {
        // Trigger analysis
        given()
                .when()
                .post("/api/v1/intelligence/process-mining/analyze");

        // Fetch insights
        Response response = given()
                .when()
                .get("/api/v1/intelligence/process-mining/insights");
        context.setResponse(response);
    }

    @When("the overall STP rate is requested")
    public void theOverallStpRateIsRequested() {
        // Trigger analysis first
        given()
                .when()
                .post("/api/v1/intelligence/process-mining/analyze");

        // Request overall STP rate (no insurer filter = aggregate)
        Response response = given()
                .when()
                .get("/api/v1/intelligence/process-mining/stp-rate");
        context.setResponse(response);
    }

    // ── New Then steps ──

    @Then("the STP rate should be {int}%")
    public void theStpRateShouldBePercent(int expectedRate) {
        Number actualRate = context.getResponse().path("overallStpRate");
        if (actualRate != null) {
            assertThat(actualRate.doubleValue()).isCloseTo(
                    (double) expectedRate, org.assertj.core.data.Offset.offset(5.0));
        } else {
            // Fallback: check under stpRate key
            Number stpRate = context.getResponse().path("stpRate");
            assertThat(stpRate).as("Neither overallStpRate nor stpRate found in response").isNotNull();
            assertThat(stpRate.doubleValue()).isCloseTo(
                    (double) expectedRate, org.assertj.core.data.Offset.offset(5.0));
        }
    }

    @Then("the rejected-to-queued transition should be identified")
    public void theRejectedToQueuedTransitionShouldBeIdentified() {
        // The deviated paths include a REJECTED -> QUEUED_FOR_BATCH transition
        // Verify via metrics endpoint
        UUID insurerId = UUID.fromString(context.retrieve("testInsurerId"));
        Response metricsResponse = given()
                .queryParam("insurerId", insurerId.toString())
                .when()
                .get("/api/v1/intelligence/process-mining/metrics");

        String body = metricsResponse.asString();
        assertThat(body).contains("REJECTED");
    }

    @Then("the insights should identify {string} as having a bottleneck")
    public void theInsightsShouldIdentifyAsHavingBottleneck(String insurerRef) {
        UUID insurerId = UUID.nameUUIDFromBytes(insurerRef.getBytes());
        List<Map<String, Object>> insights = context.getResponse().jsonPath().getList("$");
        boolean found = insights.stream()
                .anyMatch(insight -> {
                    String inId = insight.get("insurerId") != null ? insight.get("insurerId").toString() : "";
                    String type = insight.get("insightType") != null ? insight.get("insightType").toString() : "";
                    return inId.equals(insurerId.toString()) && type.equals("BOTTLENECK");
                });
        assertThat(found).as("Expected bottleneck insight for insurer %s", insurerRef).isTrue();
    }

    @Then("{string} should have no bottleneck insights")
    public void shouldHaveNoBottleneckInsights(String insurerRef) {
        UUID insurerId = UUID.nameUUIDFromBytes(insurerRef.getBytes());
        List<Map<String, Object>> insights = context.getResponse().jsonPath().getList("$");
        boolean hasBottleneck = insights.stream()
                .anyMatch(insight -> {
                    String inId = insight.get("insurerId") != null ? insight.get("insurerId").toString() : "";
                    String type = insight.get("insightType") != null ? insight.get("insightType").toString() : "";
                    return inId.equals(insurerId.toString()) && type.equals("BOTTLENECK");
                });
        assertThat(hasBottleneck).as("Expected no bottleneck insight for insurer %s", insurerRef).isFalse();
    }

    @Then("the response should include per-insurer breakdown")
    public void theResponseShouldIncludePerInsurerBreakdown() {
        Map<String, Object> perInsurer = context.getResponse().path("perInsurerStpRate");
        assertThat(perInsurer).isNotNull().isNotEmpty();
    }

    @Then("the overall rate should be the average of per-insurer rates")
    public void theOverallRateShouldBeAverageOfPerInsurerRates() {
        Number overallRate = context.getResponse().path("overallStpRate");
        Map<String, Number> perInsurer = context.getResponse().path("perInsurerStpRate");

        if (perInsurer != null && !perInsurer.isEmpty()) {
            double avg = perInsurer.values().stream()
                    .mapToDouble(Number::doubleValue)
                    .average()
                    .orElse(0);
            assertThat(overallRate.doubleValue()).isCloseTo(avg, org.assertj.core.data.Offset.offset(1.0));
        }
    }
}
