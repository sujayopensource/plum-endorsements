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

public class AnomalyDetectionSteps {

    @Autowired
    private TestContext context;

    @Autowired
    private DatabaseHelper dbHelper;

    // ── Given steps ──

    @Given("{int} ADD endorsements exist for the standard employer within {int} hour(s)")
    public void addEndorsementsExistForStandardEmployerWithinHours(int count, int hours) {
        for (int i = 0; i < count; i++) {
            UUID uniqueEmployeeId = UUID.randomUUID();
            Map<String, Object> request = buildEndorsementRequest(
                    EMPLOYER_ID, uniqueEmployeeId, INSURER_ID, POLICY_ID,
                    "ADD", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                    new BigDecimal("1200.00"), null);
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/v1/endorsements");
        }
    }

    @Given("an ADD endorsement exists for employee {string}")
    public void anAddEndorsementExistsForEmployee(String employeeRef) {
        UUID employeeId = deterministicUUID(employeeRef);
        Map<String, Object> request = buildEndorsementRequest(
                EMPLOYER_ID, employeeId, INSURER_ID, POLICY_ID,
                "ADD", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                new BigDecimal("1200.00"), "add-" + employeeRef);
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements");
    }

    @Given("a DELETE endorsement exists for employee {string}")
    public void aDeleteEndorsementExistsForEmployee(String employeeRef) {
        UUID employeeId = deterministicUUID(employeeRef);
        Map<String, Object> request = buildEndorsementRequest(
                EMPLOYER_ID, employeeId, INSURER_ID, POLICY_ID,
                "DELETE", LocalDate.of(2026, 5, 1), null,
                null, "delete-" + employeeRef);
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements");
    }

    @Given("another ADD endorsement exists for employee {string}")
    public void anotherAddEndorsementExistsForEmployee(String employeeRef) {
        UUID employeeId = deterministicUUID(employeeRef);
        Map<String, Object> request = buildEndorsementRequest(
                EMPLOYER_ID, employeeId, INSURER_ID, POLICY_ID,
                "ADD", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31),
                new BigDecimal("1500.00"), "add2-" + employeeRef);
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements");
    }

    // ── When steps ──

    @When("I request anomaly analysis for the standard employer")
    public void iRequestAnomalyAnalysisForTheStandardEmployer() {
        Response response = given()
                .queryParam("employerId", EMPLOYER_ID.toString())
                .when()
                .get("/api/v1/intelligence/anomalies");
        context.setResponse(response);
    }

    @When("I update the first anomaly status to {string} with notes {string}")
    public void iUpdateTheFirstAnomalyStatusWithNotes(String status, String notes) {
        String anomalyId = context.getResponse().path("[0].id");
        context.store("anomalyId", anomalyId);

        Map<String, Object> updateRequest = new LinkedHashMap<>();
        updateRequest.put("status", status);
        updateRequest.put("notes", notes);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .put("/api/v1/intelligence/anomalies/{anomalyId}/review", anomalyId);
        context.setResponse(response);
    }

    // ── Then steps ──

    @Then("the anomaly list should contain at least {int} entry/entries")
    public void theAnomalyListShouldContainAtLeastEntries(int minCount) {
        context.getResponse().then().body("size()", greaterThanOrEqualTo(minCount));
    }

    @Then("the anomaly list should contain an anomaly of type {string}")
    public void theAnomalyListShouldContainAnomalyOfType(String anomalyType) {
        List<String> types = context.getResponse().path("anomalyType");
        assertThat(types).contains(anomalyType);
    }

    @Then("the anomaly severity should be {string}")
    public void theAnomalySeverityShouldBe(String severity) {
        context.getResponse().then().body("[0].severity", equalTo(severity));
    }

    @Then("the anomaly status should be {string}")
    public void theAnomalyStatusShouldBe(String status) {
        context.getResponse().then().body("status", anyOf(
                equalTo(status),
                hasItem(equalTo(status))
        ));
    }

    @Then("the anomaly details should reference employee {string}")
    public void theAnomalyDetailsShouldReferenceEmployee(String employeeRef) {
        UUID employeeId = deterministicUUID(employeeRef);
        String responseBody = context.getResponse().asString();
        assertThat(responseBody).contains(employeeId.toString());
    }

    @Then("the anomaly list should be empty")
    public void theAnomalyListShouldBeEmpty() {
        context.getResponse().then().body("size()", equalTo(0));
    }

    @Then("the anomaly review notes should be {string}")
    public void theAnomalyReviewNotesShouldBe(String expectedNotes) {
        context.getResponse().then().body("reviewerNotes", equalTo(expectedNotes));
    }

    // ── New Given steps (Phase 3 Intelligence) ──

    @Given("an employer {string} with insurer {string}")
    public void anEmployerWithInsurer(String employerRef, String insurerRef) {
        UUID employerId = deterministicUUID(employerRef);
        UUID insurerId = deterministicUUID(insurerRef);
        context.store("currentEmployerId", employerId.toString());
        context.store("currentInsurerId", insurerId.toString());
    }

    @Given("the employer has an EA account with balance {double}")
    public void theEmployerHasAnEAAccountWithBalance(double balance) {
        UUID employerId = UUID.fromString(context.retrieve("currentEmployerId"));
        UUID insurerId = UUID.fromString(context.retrieve("currentInsurerId"));
        dbHelper.seedEAAccount(employerId, insurerId, BigDecimal.valueOf(balance));
    }

    @Given("an endorsement exists for the employer with type {string} created {int} days ago")
    public void anEndorsementExistsForEmployerWithTypeCreatedDaysAgo(String type, int daysAgo) {
        UUID employerId = UUID.fromString(context.retrieve("currentEmployerId"));
        UUID insurerId = UUID.fromString(context.retrieve("currentInsurerId"));
        UUID employeeId = UUID.randomUUID();
        LocalDate coverageStart = LocalDate.now().plusDays(daysAgo);

        Map<String, Object> request = buildEndorsementRequest(
                employerId, employeeId, insurerId, POLICY_ID,
                type, coverageStart, coverageStart.plusYears(1),
                new BigDecimal("1200.00"), null);
        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements");
        String endorsementId = response.path("id");
        if (endorsementId != null) {
            context.setEndorsementId(endorsementId);
        }
    }

    @Given("{int} endorsements exist for the employer in the last {int} hours")
    public void endorsementsExistForEmployerInLastHours(int count, int hours) {
        UUID employerId = UUID.fromString(context.retrieve("currentEmployerId"));
        UUID insurerId = UUID.fromString(context.retrieve("currentInsurerId"));
        for (int i = 0; i < count; i++) {
            UUID uniqueEmployeeId = UUID.randomUUID();
            Map<String, Object> request = buildEndorsementRequest(
                    employerId, uniqueEmployeeId, insurerId, POLICY_ID,
                    "ADD", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                    new BigDecimal("1200.00"), null);
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/v1/endorsements");
        }
    }

    @Given("an employee was added and then deleted within {int} days")
    public void anEmployeeWasAddedAndDeletedWithinDays(int days) {
        UUID employerId = UUID.fromString(context.retrieve("currentEmployerId"));
        UUID insurerId = UUID.fromString(context.retrieve("currentInsurerId"));
        UUID employeeId = UUID.randomUUID();
        String employeeRef = employeeId.toString().substring(0, 8);

        // Create ADD endorsement
        Map<String, Object> addRequest = buildEndorsementRequest(
                employerId, employeeId, insurerId, POLICY_ID,
                "ADD", LocalDate.now().minusDays(days), LocalDate.now().plusYears(1),
                new BigDecimal("1200.00"), "add-cycle-" + employeeRef);
        given()
                .contentType(ContentType.JSON)
                .body(addRequest)
                .when()
                .post("/api/v1/endorsements");

        // Create DELETE endorsement for the same employee
        Map<String, Object> deleteRequest = buildEndorsementRequest(
                employerId, employeeId, insurerId, POLICY_ID,
                "DELETE", LocalDate.now(), null,
                null, "delete-cycle-" + employeeRef);
        given()
                .contentType(ContentType.JSON)
                .body(deleteRequest)
                .when()
                .post("/api/v1/endorsements");
    }

    @Given("a dormant employee endorsement exists from {int} days ago")
    public void aDormantEmployeeEndorsementExistsFromDaysAgo(int daysAgo) {
        UUID employerId = UUID.fromString(context.retrieve("currentEmployerId"));
        UUID insurerId = UUID.fromString(context.retrieve("currentInsurerId"));
        UUID employeeId = UUID.randomUUID();

        // Seed an old endorsement via JDBC with created_at in the past
        UUID endorsementId = UUID.randomUUID();
        dbHelper.seedEndorsementWithCreatedAt(employerId, employeeId, insurerId, POLICY_ID, "CONFIRMED", daysAgo);

        // Now create a new endorsement for the same employee (triggers dormancy break detection)
        Map<String, Object> request = buildEndorsementRequest(
                employerId, employeeId, insurerId, POLICY_ID,
                "ADD", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                new BigDecimal("1200.00"), null);
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements");
    }

    @Given("an anomaly exists with status {string} and type {string}")
    public void anAnomalyExistsWithStatusAndType(String status, String anomalyType) {
        UUID employerId = UUID.fromString(context.retrieve("currentEmployerId"));
        UUID insurerId = UUID.fromString(context.retrieve("currentInsurerId"));

        // Ensure EA account exists for endorsement creation
        try {
            dbHelper.seedEAAccount(employerId, insurerId, new BigDecimal("500000.00"));
        } catch (Exception e) {
            // Account may already exist
        }

        // Seed endorsements to trigger anomaly detection
        for (int i = 0; i < 25; i++) {
            UUID uniqueEmployeeId = UUID.randomUUID();
            Map<String, Object> request = buildEndorsementRequest(
                    employerId, uniqueEmployeeId, insurerId, POLICY_ID,
                    "ADD", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                    new BigDecimal("1200.00"), null);
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/v1/endorsements");
        }

        // Trigger anomaly analysis
        Response analysisResponse = given()
                .queryParam("employerId", employerId.toString())
                .when()
                .get("/api/v1/intelligence/anomalies");

        // Store the anomaly ID for later use
        List<String> ids = analysisResponse.path("id");
        if (ids != null && !ids.isEmpty()) {
            context.store("anomalyId", ids.get(0));
        } else {
            // Fallback: seed an anomaly directly in the database if detection did not produce one
            UUID endorsementId = UUID.randomUUID(); // placeholder, not linked to a real endorsement
            UUID anomalyId = dbHelper.seedAnomaly(endorsementId, employerId, anomalyType, 0.85, status);
            context.store("anomalyId", anomalyId.toString());
        }
    }

    @Given("an endorsement exists for the employer with premium {double}")
    public void anEndorsementExistsForEmployerWithPremium(double premium) {
        UUID employerId = UUID.fromString(context.retrieve("currentEmployerId"));
        UUID insurerId = UUID.fromString(context.retrieve("currentInsurerId"));
        UUID employeeId = UUID.randomUUID();

        Map<String, Object> request = buildEndorsementRequest(
                employerId, employeeId, insurerId, POLICY_ID,
                "ADD", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                BigDecimal.valueOf(premium), null);
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements");
    }

    @Given("{int} endorsements exist for the employer with normal premium {double}")
    public void endorsementsExistForEmployerWithNormalPremium(int count, double premium) {
        UUID employerId = UUID.fromString(context.retrieve("currentEmployerId"));
        UUID insurerId = UUID.fromString(context.retrieve("currentInsurerId"));
        for (int i = 0; i < count; i++) {
            UUID employeeId = UUID.randomUUID();
            Map<String, Object> request = buildEndorsementRequest(
                    employerId, employeeId, insurerId, POLICY_ID,
                    "ADD", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                    BigDecimal.valueOf(premium), null);
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/v1/endorsements");
        }
    }

    // ── New When steps ──

    @When("the system analyzes the endorsement for anomalies")
    public void theSystemAnalyzesTheEndorsementForAnomalies() {
        UUID employerId = UUID.fromString(context.retrieve("currentEmployerId"));
        Response response = given()
                .queryParam("employerId", employerId.toString())
                .when()
                .get("/api/v1/intelligence/anomalies");
        context.setResponse(response);
    }

    @When("the system analyzes all endorsements for anomalies")
    public void theSystemAnalyzesAllEndorsementsForAnomalies() {
        UUID employerId = UUID.fromString(context.retrieve("currentEmployerId"));
        Response response = given()
                .queryParam("employerId", employerId.toString())
                .when()
                .get("/api/v1/intelligence/anomalies");
        context.setResponse(response);
    }

    @When("a reviewer dismisses the anomaly with notes {string}")
    public void aReviewerDismissesTheAnomalyWithNotes(String notes) {
        String anomalyId = context.retrieve("anomalyId");

        if (anomalyId == null) {
            // No anomaly was found or seeded; set a 404-style response and return
            Response response = given()
                    .when()
                    .get("/api/v1/intelligence/anomalies/" + UUID.randomUUID());
            context.setResponse(response);
            return;
        }

        Map<String, Object> reviewRequest = new LinkedHashMap<>();
        reviewRequest.put("status", "DISMISSED");
        reviewRequest.put("notes", notes);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(reviewRequest)
                .when()
                .put("/api/v1/intelligence/anomalies/{anomalyId}/review", anomalyId);
        context.setResponse(response);
    }

    // ── New Then steps ──

    @Then("an anomaly should be flagged with type {string}")
    public void anAnomalyShouldBeFlaggedWithType(String anomalyType) {
        List<String> types = context.getResponse().path("anomalyType");
        if (types != null) {
            assertThat(types).contains(anomalyType);
        } else {
            // Single response object
            String singleType = context.getResponse().path("anomalyType");
            assertThat(singleType).isEqualTo(anomalyType);
        }
    }

    @Then("the anomaly score should be at least {double}")
    public void theAnomalyScoreShouldBeAtLeast(double minScore) {
        List<Float> scores = context.getResponse().path("score");
        if (scores != null && !scores.isEmpty()) {
            assertThat(scores.stream().mapToDouble(Float::doubleValue).max().orElse(0))
                    .isGreaterThanOrEqualTo(minScore);
        } else {
            Float singleScore = context.getResponse().path("score");
            assertThat(singleScore.doubleValue()).isGreaterThanOrEqualTo(minScore);
        }
    }

    @Then("at least {int} anomalies should be flagged")
    public void atLeastAnomaliesShouldBeFlagged(int minCount) {
        context.getResponse().then().body("size()", greaterThanOrEqualTo(minCount));
    }

    @Then("anomaly types should include {string} and {string}")
    public void anomalyTypesShouldInclude(String type1, String type2) {
        List<String> types = context.getResponse().path("anomalyType");
        assertThat(types).contains(type1, type2);
    }

    @Then("the reviewer notes should be {string}")
    public void theReviewerNotesShouldBe(String expectedNotes) {
        context.getResponse().then().body("reviewerNotes", equalTo(expectedNotes));
    }

    // ── Helpers ──

    private UUID deterministicUUID(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes());
    }

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
