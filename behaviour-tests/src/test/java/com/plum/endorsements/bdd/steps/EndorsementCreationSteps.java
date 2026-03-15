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

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

public class EndorsementCreationSteps {

    static final UUID EMPLOYER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID EMPLOYEE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    static final UUID INSURER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID POLICY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private TestContext context;

    @Autowired
    private DatabaseHelper dbHelper;

    @Given("the standard test identifiers are configured")
    public void theStandardTestIdentifiersAreConfigured() {
        // UUIDs are static constants — nothing to initialize
    }

    @Given("an EA account exists with a balance of {double}")
    public void anEAAccountExistsWithBalance(double balance) {
        dbHelper.seedEAAccount(EMPLOYER_ID, INSURER_ID, BigDecimal.valueOf(balance));
    }

    @When("I create an/a {string} endorsement with premium {double}")
    public void iCreateEndorsementWithPremium(String type, double premium) {
        Map<String, Object> request = buildEndorsementRequest(type, BigDecimal.valueOf(premium), null);
        context.setRequestPayload(request);
        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements");
        context.setResponse(response);
        String id = response.path("id");
        if (id != null) {
            context.setEndorsementId(id);
        }
    }

    @When("I create an/a {string} endorsement without premium")
    public void iCreateEndorsementWithoutPremium(String type) {
        UUID uniqueEmployeeId = UUID.randomUUID();
        Map<String, Object> request = buildEndorsementRequest(
                EMPLOYER_ID, uniqueEmployeeId, INSURER_ID, POLICY_ID,
                type, LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                null, null);
        context.setRequestPayload(request);
        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements");
        context.setResponse(response);
        String id = response.path("id");
        if (id != null) {
            context.setEndorsementId(id);
        }
    }

    @Given("I create an endorsement with idempotency key {string}")
    public void iCreateEndorsementWithIdempotencyKey(String key) {
        Map<String, Object> request = buildEndorsementRequest(
                EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID,
                "ADD", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                new BigDecimal("1200.00"), key);
        context.setRequestPayload(request);
        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements");
        context.setResponse(response);
        String id = response.path("id");
        context.setEndorsementId(id);
        context.store("firstEndorsementId", id);
    }

    @When("I create another endorsement with the same idempotency key {string}")
    public void iCreateAnotherEndorsementWithSameIdempotencyKey(String key) {
        Map<String, Object> request = buildEndorsementRequest(
                EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID,
                "ADD", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                new BigDecimal("1200.00"), key);
        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements");
        context.setResponse(response);
        context.store("secondEndorsementId", response.path("id"));
    }

    @When("I create an endorsement with only the type {string}")
    public void iCreateEndorsementWithOnlyType(String type) {
        Map<String, Object> request = Map.of("type", type);
        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements");
        context.setResponse(response);
    }

    @When("I create an endorsement with invalid type {string}")
    public void iCreateEndorsementWithInvalidType(String type) {
        Map<String, Object> request = buildEndorsementRequest(
                EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID,
                type, LocalDate.of(2026, 4, 1), null,
                new BigDecimal("1000.00"), null);
        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements");
        context.setResponse(response);
    }

    @When("I create an endorsement with full details")
    public void iCreateEndorsementWithFullDetails() {
        Map<String, Object> request = buildEndorsementRequest(
                EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID,
                "ADD", LocalDate.of(2026, 6, 15), LocalDate.of(2027, 6, 14),
                new BigDecimal("2500.00"), "lifecycle-test-key-001");
        context.setRequestPayload(request);
        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements");
        context.setResponse(response);
        context.setEndorsementId(response.path("id"));
    }

    // ── Assertions ──

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int statusCode) {
        context.getResponse().then().statusCode(statusCode);
    }

    @Then("the response status code should be {int} or {int}")
    public void theResponseStatusCodeShouldBeEither(int code1, int code2) {
        int actual = context.getResponse().statusCode();
        assertThat(actual).isIn(code1, code2);
    }

    @Then("the endorsement status should be {string}")
    public void theEndorsementStatusShouldBe(String status) {
        context.getResponse().then().body("status", equalTo(status));
    }

    @Then("the endorsement type should be {string}")
    public void theEndorsementTypeShouldBe(String type) {
        context.getResponse().then().body("type", equalTo(type));
    }

    @Then("the response should contain a non-null {string}")
    public void theResponseShouldContainNonNull(String field) {
        context.getResponse().then().body(field, notNullValue());
    }

    @Then("the idempotency key should be generated from request fields")
    public void theIdempotencyKeyShouldBeGenerated() {
        String expectedKey = EMPLOYER_ID + "-" + EMPLOYEE_ID + "-ADD-2026-04-01";
        context.getResponse().then().body("idempotencyKey", equalTo(expectedKey));
    }

    @Then("both responses should return the same endorsement ID")
    public void bothResponsesShouldReturnSameId() {
        assertThat(context.retrieve("secondEndorsementId"))
                .isEqualTo(context.retrieve("firstEndorsementId"));
    }

    @Then("the response should contain a {string} title")
    public void theResponseShouldContainTitle(String title) {
        context.getResponse().then().body("title", equalTo(title));
    }

    // ── Request builder helpers ──

    private Map<String, Object> buildEndorsementRequest(String type, BigDecimal premium, String idempotencyKey) {
        return buildEndorsementRequest(EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID,
                type, LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31), premium, idempotencyKey);
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
