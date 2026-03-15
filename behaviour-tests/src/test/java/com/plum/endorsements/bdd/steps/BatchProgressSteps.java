package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.DatabaseHelper;
import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static com.plum.endorsements.bdd.steps.EndorsementCreationSteps.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class BatchProgressSteps {

    @Autowired
    private TestContext context;

    @Autowired
    private DatabaseHelper dbHelper;

    @Autowired
    private JdbcTemplate jdbc;

    @Given("a batch exists with endorsements for the standard employer")
    public void aBatchExistsWithEndorsementsForStandardEmployer() {
        createBatchWithEndorsements(1);
    }

    @Given("{int} batches exist with endorsements for the standard employer")
    public void batchesExistWithEndorsementsForStandardEmployer(int count) {
        createBatchWithEndorsements(count);
    }

    @When("I list batch progress for the standard employer")
    public void iListBatchProgressForStandardEmployer() {
        Response response = given()
                .when()
                .get("/api/v1/endorsements/employers/{employerId}/batches", EMPLOYER_ID.toString());
        context.setResponse(response);
    }

    @When("I list batch progress with page {int} and size {int}")
    public void iListBatchProgressWithPageAndSize(int page, int size) {
        Response response = given()
                .queryParam("page", page)
                .queryParam("size", size)
                .when()
                .get("/api/v1/endorsements/employers/{employerId}/batches", EMPLOYER_ID.toString());
        context.setResponse(response);
    }

    @Then("the batch progress count should be {int}")
    public void theBatchProgressCountShouldBe(int count) {
        context.getResponse().then().body("content.size()", equalTo(count));
    }

    @Then("the first batch should have status {string}")
    public void theFirstBatchShouldHaveStatus(String status) {
        context.getResponse().then().body("content[0].status", equalTo(status));
    }

    private void createBatchWithEndorsements(int batchCount) {
        for (int b = 0; b < batchCount; b++) {
            UUID batchId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO endorsement_batches (id, insurer_id, status, endorsement_count, total_premium,
                        insurer_batch_ref, created_at)
                    VALUES (?, ?, 'SUBMITTED', 2, 2000.00, ?, now())
                    """,
                    batchId, INSURER_ID, "BATCH-BDD-" + b);

            // Link endorsements to the batch
            for (int e = 0; e < 2; e++) {
                UUID endorsementId = UUID.randomUUID();
                jdbc.update("""
                        INSERT INTO endorsements (id, employer_id, employee_id, insurer_id, policy_id,
                            type, status, coverage_start_date, employee_data, premium_amount,
                            idempotency_key, retry_count, batch_id, created_at, updated_at, version)
                        VALUES (?, ?, ?, ?, ?, 'ADD', 'BATCH_SUBMITTED', '2026-04-01',
                            '{"name":"Test Employee"}'::jsonb, 1000.00, ?, 0, ?, now(), now(), 0)
                        """,
                        endorsementId, EMPLOYER_ID, UUID.randomUUID(), INSURER_ID, POLICY_ID,
                        "key-" + endorsementId, batchId);
            }
        }
    }
}
