package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.DatabaseHelper;
import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class AuditLoggingSteps {

    private static final UUID EMPLOYER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EMPLOYEE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID INSURER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID POLICY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private TestContext testContext;

    @Autowired
    private DatabaseHelper databaseHelper;

    @When("I request the audit logs")
    public void iRequestAuditLogs() {
        var response = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/audit-logs");
        testContext.setResponse(response);
    }

    @Then("the audit log response should be paginated")
    public void auditLogResponseShouldBePaginated() {
        var json = testContext.getResponse().jsonPath();
        assertThat((Object) json.get("content")).isNotNull();
        assertThat((Object) json.get("totalElements")).isNotNull();
        assertThat((Object) json.get("totalPages")).isNotNull();
    }

    @When("I create an ADD endorsement with premium {float}")
    public void iCreateEndorsement(float premium) {
        databaseHelper.seedEAAccount(EMPLOYER_ID, INSURER_ID, new BigDecimal("100000.00"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("employerId", EMPLOYER_ID.toString());
        request.put("employeeId", EMPLOYEE_ID.toString());
        request.put("insurerId", INSURER_ID.toString());
        request.put("policyId", POLICY_ID.toString());
        request.put("type", "ADD");
        request.put("coverageStartDate", LocalDate.of(2026, 4, 1).toString());
        request.put("coverageEndDate", LocalDate.of(2027, 3, 31).toString());
        request.put("employeeData", Map.of("name", "Test", "dob", "1990-05-15", "gender", "M"));
        request.put("premiumAmount", premium);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/endorsements")
                .then()
                .statusCode(201);
    }

    @Then("the audit logs should contain at least {int} entry")
    public void auditLogsShouldContainAtLeast(int count) {
        int total = testContext.getResponse().jsonPath().getInt("totalElements");
        assertThat(total).isGreaterThanOrEqualTo(count);
    }
}
