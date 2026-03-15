package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class EndorsementConfirmRejectSteps {

    @Autowired
    private TestContext context;

    @When("I confirm the endorsement with insurer reference {string}")
    public void iConfirmTheEndorsementWithInsurerReference(String insurerRef) {
        Response response = given()
                .queryParam("insurerReference", insurerRef)
                .when()
                .post("/api/v1/endorsements/{id}/confirm", context.getEndorsementId());
        context.setResponse(response);
    }

    @When("I confirm a non-existent endorsement with insurer reference {string}")
    public void iConfirmNonExistentEndorsement(String insurerRef) {
        Response response = given()
                .queryParam("insurerReference", insurerRef)
                .when()
                .post("/api/v1/endorsements/{id}/confirm", UUID.randomUUID().toString());
        context.setResponse(response);
    }

    @When("I reject the endorsement with reason {string}")
    public void iRejectTheEndorsementWithReason(String reason) {
        Response response = given()
                .queryParam("reason", reason)
                .when()
                .post("/api/v1/endorsements/{id}/reject", context.getEndorsementId());
        context.setResponse(response);
    }

    @When("I reject a non-existent endorsement with reason {string}")
    public void iRejectNonExistentEndorsement(String reason) {
        Response response = given()
                .queryParam("reason", reason)
                .when()
                .post("/api/v1/endorsements/{id}/reject", UUID.randomUUID().toString());
        context.setResponse(response);
    }

    @Then("the endorsement insurer reference should be {string}")
    public void theEndorsementInsurerReferenceShouldBe(String expectedRef) {
        given()
                .when()
                .get("/api/v1/endorsements/{id}", context.getEndorsementId())
                .then()
                .statusCode(200)
                .body("insurerReference", equalTo(expectedRef));
    }

    @Then("the endorsement retry count should be {int}")
    public void theEndorsementRetryCountShouldBe(int expectedCount) {
        given()
                .when()
                .get("/api/v1/endorsements/{id}", context.getEndorsementId())
                .then()
                .statusCode(200)
                .body("retryCount", equalTo(expectedCount));
    }

    @Then("the endorsement failure reason should be {string}")
    public void theEndorsementFailureReasonShouldBe(String expectedReason) {
        given()
                .when()
                .get("/api/v1/endorsements/{id}", context.getEndorsementId())
                .then()
                .statusCode(200)
                .body("failureReason", equalTo(expectedReason));
    }
}
