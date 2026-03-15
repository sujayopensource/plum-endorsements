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

public class EndorsementSubmissionSteps {

    @Autowired
    private TestContext context;

    @Autowired
    private DatabaseHelper dbHelper;

    @Given("an existing endorsement in {string} status")
    public void anExistingEndorsementInStatus(String status) {
        UUID id = dbHelper.seedEndorsementAtStatus(EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID, status, 0);
        context.setEndorsementId(id.toString());
    }

    @Given("an existing endorsement in {string} status with retry count {int}")
    public void anExistingEndorsementInStatusWithRetryCount(String status, int retryCount) {
        UUID id = dbHelper.seedEndorsementAtStatus(EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID, status, retryCount);
        context.setEndorsementId(id.toString());
    }

    @When("I submit the endorsement to the insurer")
    public void iSubmitTheEndorsementToTheInsurer() {
        Response response = given()
                .when()
                .post("/api/v1/endorsements/{id}/submit", context.getEndorsementId());
        context.setResponse(response);
    }

    @When("I submit a non-existent endorsement to the insurer")
    public void iSubmitNonExistentEndorsement() {
        Response response = given()
                .when()
                .post("/api/v1/endorsements/{id}/submit", UUID.randomUUID().toString());
        context.setResponse(response);
    }

    @Then("the endorsement should be in {string} status")
    public void theEndorsementShouldBeInStatus(String expectedStatus) {
        given()
                .when()
                .get("/api/v1/endorsements/{id}", context.getEndorsementId())
                .then()
                .statusCode(200)
                .body("status", equalTo(expectedStatus));
    }

    @Then("the endorsement should have an insurer reference starting with {string}")
    public void theEndorsementShouldHaveInsurerReferenceStartingWith(String prefix) {
        given()
                .when()
                .get("/api/v1/endorsements/{id}", context.getEndorsementId())
                .then()
                .statusCode(200)
                .body("insurerReference", startsWith(prefix));
    }
}
