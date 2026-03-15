package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.DatabaseHelper;
import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static com.plum.endorsements.bdd.steps.EndorsementCreationSteps.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class OutstandingItemsSteps {

    @Autowired
    private TestContext context;

    @Autowired
    private DatabaseHelper dbHelper;

    @Given("{int} endorsements exist with status {string}")
    public void endorsementsExistWithStatus(int count, String status) {
        for (int i = 0; i < count; i++) {
            dbHelper.seedEndorsementAtStatus(
                    EMPLOYER_ID, UUID.randomUUID(), INSURER_ID, POLICY_ID, status, 0);
        }
    }

    @When("I list outstanding items for the standard employer")
    public void iListOutstandingItemsForStandardEmployer() {
        Response response = given()
                .when()
                .get("/api/v1/endorsements/employers/{employerId}/outstanding", EMPLOYER_ID.toString());
        context.setResponse(response);
    }

    @When("I list outstanding items with page {int} and size {int}")
    public void iListOutstandingItemsWithPageAndSize(int page, int size) {
        Response response = given()
                .queryParam("page", page)
                .queryParam("size", size)
                .when()
                .get("/api/v1/endorsements/employers/{employerId}/outstanding", EMPLOYER_ID.toString());
        context.setResponse(response);
    }

    @Then("the outstanding items count should be {int}")
    public void theOutstandingItemsCountShouldBe(int count) {
        context.getResponse().then().body("content.size()", equalTo(count));
    }
}
