package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static com.plum.endorsements.bdd.steps.EndorsementCreationSteps.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;

public class EndorsementLifecycleSteps {

    @Autowired
    private TestContext context;

    @Then("all endorsement fields should match the original request")
    public void allEndorsementFieldsShouldMatchOriginalRequest() {
        Map<String, Object> request = context.getRequestPayload();

        context.getResponse().then()
                .body("id", equalTo(context.getEndorsementId()))
                .body("employerId", equalTo(EMPLOYER_ID.toString()))
                .body("employeeId", equalTo(EMPLOYEE_ID.toString()))
                .body("insurerId", equalTo(INSURER_ID.toString()))
                .body("policyId", equalTo(POLICY_ID.toString()))
                .body("type", equalTo(request.get("type")))
                .body("status", equalTo("PROVISIONALLY_COVERED"))
                .body("coverageStartDate", equalTo(request.get("coverageStartDate")))
                .body("coverageEndDate", equalTo(request.get("coverageEndDate")))
                .body("premiumAmount", equalTo(2500.00f))
                .body("batchId", nullValue())
                .body("insurerReference", nullValue())
                .body("retryCount", equalTo(0))
                .body("failureReason", nullValue())
                .body("idempotencyKey", equalTo(request.get("idempotencyKey")))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue());
    }
}
