package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class ProvisionalCoverageSteps {

    @Autowired
    private TestContext context;

    @When("I get the provisional coverage for the endorsement")
    public void iGetTheProvisionalCoverageForTheEndorsement() {
        Response response = given()
                .when()
                .get("/api/v1/endorsements/{id}/coverage", context.getEndorsementId());
        context.setResponse(response);
    }

    @Then("no provisional coverage should exist for this endorsement")
    public void noProvisionalCoverageShouldExist() {
        given()
                .when()
                .get("/api/v1/endorsements/{id}/coverage", context.getEndorsementId())
                .then()
                .statusCode(404);
    }

    @Then("the coverage type should be {string}")
    public void theCoverageTypeShouldBe(String expectedType) {
        context.getResponse().then().body("coverageType", equalTo(expectedType));
    }

    @Then("the coverage endorsement ID should match")
    public void theCoverageEndorsementIdShouldMatch() {
        context.getResponse().then().body("endorsementId", equalTo(context.getEndorsementId()));
    }

    @Then("the coverage should have a non-null confirmed date")
    public void theCoverageShouldHaveNonNullConfirmedDate() {
        context.getResponse().then().body("confirmedAt", notNullValue());
    }
}
