package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.DatabaseHelper;
import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static com.plum.endorsements.bdd.steps.EndorsementCreationSteps.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class ReconciliationSteps {

    private static final UUID MOCK_INSURER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private TestContext context;

    @Autowired
    private DatabaseHelper dbHelper;

    // ── Given steps ──

    @Given("a CONFIRMED endorsement exists for the mock insurer")
    public void aConfirmedEndorsementExistsForTheMockInsurer() {
        UUID id = dbHelper.seedEndorsementAtStatus(
                EMPLOYER_ID, UUID.randomUUID(), MOCK_INSURER_ID, POLICY_ID, "CONFIRMED", 0);
        context.setEndorsementId(id.toString());
    }

    // ── When steps ──

    @When("I trigger reconciliation for the mock insurer")
    public void iTriggerReconciliationForTheMockInsurer() {
        Response response = given()
                .queryParam("insurerId", MOCK_INSURER_ID.toString())
                .when()
                .post("/api/v1/reconciliation/trigger");
        context.setResponse(response);
    }

    @When("I list reconciliation runs for the mock insurer")
    public void iListReconciliationRunsForTheMockInsurer() {
        Response response = given()
                .queryParam("insurerId", MOCK_INSURER_ID.toString())
                .when()
                .get("/api/v1/reconciliation/runs");
        context.setResponse(response);
    }

    @When("I list reconciliation runs for a random insurer")
    public void iListReconciliationRunsForARandomInsurer() {
        Response response = given()
                .queryParam("insurerId", UUID.randomUUID().toString())
                .when()
                .get("/api/v1/reconciliation/runs");
        context.setResponse(response);
    }

    @When("I get the reconciliation run items for the first run")
    public void iGetTheReconciliationRunItemsForTheFirstRun() {
        String runId = context.getResponse().path("id");
        Response response = given()
                .when()
                .get("/api/v1/reconciliation/runs/{runId}/items", runId);
        context.setResponse(response);
    }

    // ── Then steps ──

    @Then("the response body should contain field {string}")
    public void theResponseBodyShouldContainField(String field) {
        context.getResponse().then().body(field, notNullValue());
    }

    @Then("the response body list size should be at least {int}")
    public void theResponseBodyListSizeShouldBeAtLeast(int minSize) {
        context.getResponse().then().body("size()", greaterThanOrEqualTo(minSize));
    }

    @Then("the response body list should be empty")
    public void theResponseBodyListShouldBeEmpty() {
        context.getResponse().then().body("size()", equalTo(0));
    }
}
