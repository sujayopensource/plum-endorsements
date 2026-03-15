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
import java.util.Map;
import java.util.UUID;

import static com.plum.endorsements.bdd.steps.EndorsementCreationSteps.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;

public class EndorsementQuerySteps {

    @Autowired
    private TestContext context;

    @Autowired
    private DatabaseHelper dbHelper;

    @Given("an existing endorsement created via the API")
    public void anExistingEndorsementCreatedViaApi() {
        Map<String, Object> request = buildRequest("ADD", new BigDecimal("1200.00"));
        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements");
        context.setEndorsementId(response.path("id"));
    }

    @Given("{int} endorsements exist for the standard employer")
    public void endorsementsExistForStandardEmployer(int count) {
        for (int i = 0; i < count; i++) {
            UUID uniqueEmployeeId = UUID.randomUUID();
            Map<String, Object> request = buildRequest(
                    EMPLOYER_ID, uniqueEmployeeId, INSURER_ID, POLICY_ID,
                    "ADD", LocalDate.of(2026, 4, 1 + i), null,
                    new BigDecimal("1000.00"), null);
            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/v1/endorsements")
                    .then()
                    .statusCode(201);
        }
    }

    @Given("an endorsement exists with status {string}")
    public void anEndorsementExistsWithStatus(String status) {
        UUID id = dbHelper.seedEndorsementAtStatus(
                EMPLOYER_ID, UUID.randomUUID(), INSURER_ID, POLICY_ID, status, 0);
        // Store the latest ID if needed
        context.setEndorsementId(id.toString());
    }

    @When("I get the endorsement by its ID")
    public void iGetTheEndorsementByItsId() {
        Response response = given()
                .when()
                .get("/api/v1/endorsements/{id}", context.getEndorsementId());
        context.setResponse(response);
    }

    @When("I get an endorsement with a random UUID")
    public void iGetEndorsementWithRandomUuid() {
        Response response = given()
                .when()
                .get("/api/v1/endorsements/{id}", UUID.randomUUID().toString());
        context.setResponse(response);
    }

    @When("I list endorsements for the standard employer")
    public void iListEndorsementsForStandardEmployer() {
        Response response = given()
                .queryParam("employerId", EMPLOYER_ID.toString())
                .when()
                .get("/api/v1/endorsements");
        context.setResponse(response);
    }

    @When("I list endorsements with status filter {string}")
    public void iListEndorsementsWithStatusFilter(String status) {
        Response response = given()
                .queryParam("employerId", EMPLOYER_ID.toString())
                .queryParam("statuses", status)
                .when()
                .get("/api/v1/endorsements");
        context.setResponse(response);
    }

    @When("I list endorsements with page {int} and size {int}")
    public void iListEndorsementsWithPageAndSize(int page, int size) {
        Response response = given()
                .queryParam("employerId", EMPLOYER_ID.toString())
                .queryParam("page", page)
                .queryParam("size", size)
                .when()
                .get("/api/v1/endorsements");
        context.setResponse(response);
    }

    @When("I list endorsements for a random employer")
    public void iListEndorsementsForRandomEmployer() {
        Response response = given()
                .queryParam("employerId", UUID.randomUUID().toString())
                .when()
                .get("/api/v1/endorsements");
        context.setResponse(response);
    }

    @Then("the response should contain {int} endorsements")
    public void theResponseShouldContainEndorsements(int count) {
        context.getResponse().then().body("content.size()", equalTo(count));
    }

    @Then("the total elements should be {int}")
    public void theTotalElementsShouldBe(int count) {
        context.getResponse().then().body("totalElements", equalTo(count));
    }

    @Then("the total pages should be {int}")
    public void theTotalPagesShouldBe(int count) {
        context.getResponse().then().body("totalPages", equalTo(count));
    }

    @Then("all returned endorsements should have status {string}")
    public void allReturnedEndorsementsShouldHaveStatus(String status) {
        context.getResponse().then().body("content.status", everyItem(equalTo(status)));
    }

    // ── Request builder helpers ──

    private Map<String, Object> buildRequest(String type, BigDecimal premium) {
        return buildRequest(EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID,
                type, LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31), premium, null);
    }

    private Map<String, Object> buildRequest(
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
