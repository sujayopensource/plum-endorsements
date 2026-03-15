package com.plum.endorsements.apitest.tests;

import com.plum.endorsements.apitest.base.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Endorsement API")
@Feature("Provisional Coverage")
@DisplayName("GET /api/v1/endorsements/{id}/coverage")
class ProvisionalCoverageApiTest extends BaseApiTest {

    @Test
    @DisplayName("Should return provisional coverage for ADD endorsement")
    @Description("ADD endorsements create a provisional coverage record accessible via GET /coverage")
    void shouldReturnProvisionalCoverage_ForAddEndorsement() {
        String id = createEndorsementAndGetId("ADD", new BigDecimal("1000.00"));

        given()
                .when()
                .get("/api/v1/endorsements/{id}/coverage", id)
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("endorsementId", equalTo(id))
                .body("employeeId", equalTo(EMPLOYEE_ID.toString()))
                .body("employerId", equalTo(EMPLOYER_ID.toString()))
                .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("Should return 404 for DELETE endorsement coverage")
    @Description("DELETE endorsements do not create provisional coverage records")
    void shouldReturn404_ForDeleteEndorsement() {
        Map<String, Object> request = createEndorsementRequest(
                EMPLOYER_ID, UUID.randomUUID(), INSURER_ID, POLICY_ID,
                "DELETE", LocalDate.of(2026, 4, 1), null,
                null, null);

        String id = createEndorsementViaApi(request)
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .get("/api/v1/endorsements/{id}/coverage", id)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Should return confirmed coverage after submission")
    @Description("After submit (which auto-confirms via mock insurer), coverage type should be CONFIRMED")
    void shouldReturnConfirmedCoverage_AfterSubmission() {
        String id = createEndorsementAndGetId("ADD", new BigDecimal("1000.00"));

        // Submit (auto-confirms via MockInsurerAdapter)
        given().post("/api/v1/endorsements/{id}/submit", id)
                .then().statusCode(202);

        given()
                .when()
                .get("/api/v1/endorsements/{id}/coverage", id)
                .then()
                .statusCode(200)
                .body("endorsementId", equalTo(id))
                .body("coverageType", equalTo("CONFIRMED"))
                .body("confirmedAt", notNullValue());
    }
}
