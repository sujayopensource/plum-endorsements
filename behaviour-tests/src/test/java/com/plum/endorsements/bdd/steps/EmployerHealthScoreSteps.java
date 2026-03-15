package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.DatabaseHelper;
import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class EmployerHealthScoreSteps {

    private static final UUID EMPLOYER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EMPLOYEE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID INSURER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID POLICY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private TestContext testContext;

    @Autowired
    private DatabaseHelper databaseHelper;

    @Given("an EA account exists for the standard employer with balance {double}")
    public void anEAAccountExistsForStandardEmployer(double balance) {
        databaseHelper.seedEAAccount(EMPLOYER_ID, INSURER_ID, BigDecimal.valueOf(balance));
    }

    @Given("a CONFIRMED endorsement exists for the standard employer")
    public void aConfirmedEndorsementExists() {
        databaseHelper.seedEndorsementAtStatus(EMPLOYER_ID, EMPLOYEE_ID, INSURER_ID, POLICY_ID, "CONFIRMED", 0);
    }

    @When("I request the health score for the standard employer")
    public void iRequestHealthScore() {
        var response = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/intelligence/employers/{id}/health-score", EMPLOYER_ID);
        testContext.setResponse(response);
    }

    @Then("the health score overall should be at least {int}")
    public void healthScoreOverallShouldBeAtLeast(int min) {
        float score = testContext.getResponse().jsonPath().getFloat("overallScore");
        assertThat(score).isGreaterThanOrEqualTo(min);
    }

    @Then("the health score risk level should be {string}")
    public void healthScoreRiskLevelShouldBe(String level) {
        String riskLevel = testContext.getResponse().jsonPath().getString("riskLevel");
        assertThat(riskLevel).isEqualTo(level);
    }

    @Then("the endorsement success rate should be {float}")
    public void endorsementSuccessRateShouldBe(float rate) {
        float actual = testContext.getResponse().jsonPath().getFloat("endorsementSuccessRate");
        assertThat(actual).isEqualTo(rate);
    }

    @Then("the balance health score should be {float}")
    public void balanceHealthScoreShouldBe(float score) {
        float actual = testContext.getResponse().jsonPath().getFloat("balanceHealthScore");
        assertThat(actual).isEqualTo(score);
    }

    @Then("the health score response should contain all components")
    public void healthScoreResponseShouldContainAllComponents() {
        var response = testContext.getResponse().jsonPath();
        assertThat((Object) response.get("endorsementSuccessRate")).isNotNull();
        assertThat((Object) response.get("anomalyScore")).isNotNull();
        assertThat((Object) response.get("balanceHealthScore")).isNotNull();
        assertThat((Object) response.get("reconciliationScore")).isNotNull();
        assertThat((Object) response.get("calculatedAt")).isNotNull();
    }
}
