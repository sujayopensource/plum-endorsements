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
@Feature("Get Endorsements")
@DisplayName("GET /api/v1/endorsements")
class GetEndorsementApiTest extends BaseApiTest {

    @Test
    @DisplayName("Should return endorsement when it exists")
    @Description("GET by ID returns the full endorsement response")
    void shouldReturnEndorsement_WhenExists() {
        String id = createEndorsementAndGetId("ADD", new BigDecimal("1000.00"));

        given()
                .when()
                .get("/api/v1/endorsements/{id}", id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("type", equalTo("ADD"))
                .body("status", equalTo("PROVISIONALLY_COVERED"))
                .body("employerId", equalTo(EMPLOYER_ID.toString()))
                .body("employeeId", equalTo(EMPLOYEE_ID.toString()))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue());
    }

    @Test
    @DisplayName("Should return 404 when endorsement not found")
    @Description("GET with a non-existent UUID returns ProblemDetail 404")
    void shouldReturn404_WhenEndorsementNotFound() {
        given()
                .when()
                .get("/api/v1/endorsements/{id}", UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("title", equalTo("Endorsement Not Found"));
    }

    @Test
    @DisplayName("Should list endorsements by employer ID")
    @Description("GET list returns paginated results filtered by employerId")
    void shouldListEndorsements_ByEmployerId() {
        // Create 3 endorsements with different employees
        for (int i = 0; i < 3; i++) {
            Map<String, Object> request = createEndorsementRequest(
                    EMPLOYER_ID, UUID.randomUUID(), INSURER_ID, POLICY_ID,
                    "ADD", LocalDate.of(2026, 4, 1 + i), null,
                    new BigDecimal("1000.00"), null);
            createEndorsementViaApi(request).statusCode(201);
        }

        given()
                .queryParam("employerId", EMPLOYER_ID)
                .when()
                .get("/api/v1/endorsements")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(3))
                .body("totalElements", equalTo(3))
                .body("number", equalTo(0));
    }

    @Test
    @DisplayName("Should filter endorsements by status")
    @Description("GET list with statuses parameter returns only matching endorsements")
    void shouldListEndorsements_FilterByStatus() {
        // Create an endorsement (will be PROVISIONALLY_COVERED)
        Map<String, Object> request1 = createEndorsementRequest(
                EMPLOYER_ID, UUID.randomUUID(), INSURER_ID, POLICY_ID,
                "ADD", LocalDate.of(2026, 4, 1), null,
                new BigDecimal("1000.00"), null);
        createEndorsementViaApi(request1).statusCode(201);

        // Create and submit another (will be CONFIRMED via mock insurer)
        Map<String, Object> request2 = createEndorsementRequest(
                EMPLOYER_ID, UUID.randomUUID(), INSURER_ID, POLICY_ID,
                "ADD", LocalDate.of(2026, 5, 1), null,
                new BigDecimal("1000.00"), null);
        String submittedId = createEndorsementViaApi(request2)
                .statusCode(201)
                .extract().path("id");

        given().post("/api/v1/endorsements/{id}/submit", submittedId)
                .then().statusCode(202);

        // Filter by PROVISIONALLY_COVERED only
        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("statuses", "PROVISIONALLY_COVERED")
                .when()
                .get("/api/v1/endorsements")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].status", equalTo("PROVISIONALLY_COVERED"));
    }

    @Test
    @DisplayName("Should paginate endorsements correctly")
    @Description("GET list respects page and size parameters")
    void shouldListEndorsements_Paginated() {
        // Create 5 endorsements
        for (int i = 0; i < 5; i++) {
            Map<String, Object> request = createEndorsementRequest(
                    EMPLOYER_ID, UUID.randomUUID(), INSURER_ID, POLICY_ID,
                    "ADD", LocalDate.of(2026, 4, 1 + i), null,
                    new BigDecimal("1000.00"), null);
            createEndorsementViaApi(request).statusCode(201);
        }

        // Request page 0 with size 2
        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("page", 0)
                .queryParam("size", 2)
                .when()
                .get("/api/v1/endorsements")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("totalElements", equalTo(5))
                .body("totalPages", equalTo(3))
                .body("number", equalTo(0))
                .body("size", equalTo(2));

        // Request page 2 (last page, should have 1 element)
        given()
                .queryParam("employerId", EMPLOYER_ID)
                .queryParam("page", 2)
                .queryParam("size", 2)
                .when()
                .get("/api/v1/endorsements")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("number", equalTo(2));
    }

    @Test
    @DisplayName("Should return empty page when no endorsements for employer")
    @Description("GET list for an employer with no endorsements returns an empty page")
    void shouldReturnEmptyPage_WhenNoEndorsementsForEmployer() {
        given()
                .queryParam("employerId", UUID.randomUUID())
                .when()
                .get("/api/v1/endorsements")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0))
                .body("totalElements", equalTo(0));
    }
}
