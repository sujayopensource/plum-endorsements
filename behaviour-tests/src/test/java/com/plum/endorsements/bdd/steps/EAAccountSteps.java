package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static com.plum.endorsements.bdd.steps.EndorsementCreationSteps.EMPLOYER_ID;
import static com.plum.endorsements.bdd.steps.EndorsementCreationSteps.INSURER_ID;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class EAAccountSteps {

    @Autowired
    private TestContext context;

    @When("I get the EA account for the standard employer and insurer")
    public void iGetTheEAAccountForStandardEmployerAndInsurer() {
        Response response = fetchEAAccount();
        context.setResponse(response);
    }

    @When("I get an EA account with random employer and insurer IDs")
    public void iGetEAAccountWithRandomIds() {
        Response response = given()
                .queryParam("employerId", UUID.randomUUID().toString())
                .queryParam("insurerId", UUID.randomUUID().toString())
                .when()
                .get("/api/v1/ea-accounts");
        context.setResponse(response);
    }

    @Then("the EA account balance should be {double}")
    public void theEAAccountBalanceShouldBe(double expectedBalance) {
        fetchEAAccount().then().body("balance", equalTo((float) expectedBalance));
    }

    @Then("the EA account reserved amount should be {double}")
    public void theEAAccountReservedAmountShouldBe(double expectedReserved) {
        fetchEAAccount().then().body("reserved", equalTo((float) expectedReserved));
    }

    @Then("the EA account available balance should be {double}")
    public void theEAAccountAvailableBalanceShouldBe(double expectedAvailable) {
        fetchEAAccount().then().body("availableBalance", equalTo((float) expectedAvailable));
    }

    private Response fetchEAAccount() {
        return given()
                .queryParam("employerId", EMPLOYER_ID.toString())
                .queryParam("insurerId", INSURER_ID.toString())
                .when()
                .get("/api/v1/ea-accounts");
    }
}
