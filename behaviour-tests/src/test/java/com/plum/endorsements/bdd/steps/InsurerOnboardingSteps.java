package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class InsurerOnboardingSteps {

    @Autowired
    private TestContext testContext;

    @When("I create an insurer with name {string} and code {string}")
    public void iCreateInsurer(String name, String code) {
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "insurerName", name,
                        "insurerCode", code,
                        "adapterType", "MOCK",
                        "supportsRealTime", true,
                        "supportsBatch", false,
                        "maxBatchSize", 50,
                        "batchSlaHours", 24,
                        "rateLimitPerMinute", 30
                ))
                .when()
                .post("/api/v1/insurers");
        testContext.setResponse(response);
    }

    @Then("the insurer name should be {string}")
    public void insurerNameShouldBe(String name) {
        String actual = testContext.getResponse().jsonPath().getString("insurerName");
        assertThat(actual).isEqualTo(name);
    }

    @Then("the insurer code should be {string}")
    public void insurerCodeShouldBe(String code) {
        String actual = testContext.getResponse().jsonPath().getString("insurerCode");
        assertThat(actual).isEqualTo(code);
    }

    @Then("the insurer should be active")
    public void insurerShouldBeActive() {
        boolean active = testContext.getResponse().jsonPath().getBoolean("active");
        assertThat(active).isTrue();
    }

    @Then("the insurer should not be active")
    public void insurerShouldNotBeActive() {
        boolean active = testContext.getResponse().jsonPath().getBoolean("active");
        assertThat(active).isFalse();
    }

    @Given("a new insurer {string} with code {string} exists")
    public void aNewInsurerExists(String name, String code) {
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "insurerName", name,
                        "insurerCode", code,
                        "adapterType", "MOCK",
                        "supportsRealTime", true,
                        "supportsBatch", false,
                        "maxBatchSize", 50,
                        "batchSlaHours", 24,
                        "rateLimitPerMinute", 30
                ))
                .when()
                .post("/api/v1/insurers");
        String insurerId = response.jsonPath().getString("insurerId");
        testContext.store("newInsurerId", insurerId);
    }

    @When("I update the insurer name to {string}")
    public void iUpdateInsurerName(String name) {
        String insurerId = testContext.retrieve("newInsurerId");
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("insurerName", name))
                .when()
                .put("/api/v1/insurers/{id}", insurerId);
        testContext.setResponse(response);
    }

    @When("I deactivate the insurer")
    public void iDeactivateInsurer() {
        String insurerId = testContext.retrieve("newInsurerId");
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("active", false))
                .when()
                .put("/api/v1/insurers/{id}", insurerId);
        testContext.setResponse(response);
    }
}
