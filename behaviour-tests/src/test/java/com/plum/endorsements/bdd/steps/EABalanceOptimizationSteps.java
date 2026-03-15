package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.plum.endorsements.bdd.steps.EndorsementCreationSteps.*;

public class EABalanceOptimizationSteps {

    @Autowired
    private TestContext context;

    @When("I create another ADD endorsement with premium {double}")
    public void iCreateAnotherAddEndorsementWithPremium(double premium) {
        UUID uniqueEmployeeId = UUID.randomUUID();
        Map<String, Object> request = buildEndorsementRequest(
                EMPLOYER_ID, uniqueEmployeeId, INSURER_ID, POLICY_ID,
                "ADD", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                BigDecimal.valueOf(premium), null);
        context.setRequestPayload(request);
        Response response = io.restassured.RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements");
        context.setResponse(response);
        String id = response.path("id");
        if (id != null) {
            context.setEndorsementId(id);
        }
    }

    // ── Request builder helper ──

    private Map<String, Object> buildEndorsementRequest(
            UUID employerId, UUID employeeId, UUID insurerId, UUID policyId,
            String type, LocalDate coverageStart, LocalDate coverageEnd,
            BigDecimal premiumAmount, String idempotencyKey) {

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("employerId", employerId.toString());
        request.put("employeeId", employeeId.toString());
        request.put("insurerId", insurerId.toString());
        request.put("policyId", policyId.toString());
        request.put("type", type);
        request.put("coverageStartDate", coverageStart.toString());
        if (coverageEnd != null) {
            request.put("coverageEndDate", coverageEnd.toString());
        }
        request.put("employeeData", Map.of(
                "name", "Test Employee",
                "dob", "1990-05-15",
                "gender", "M"
        ));
        if (premiumAmount != null) {
            request.put("premiumAmount", premiumAmount);
        }
        if (idempotencyKey != null) {
            request.put("idempotencyKey", idempotencyKey);
        }
        return request;
    }
}
