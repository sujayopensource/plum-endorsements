package com.plum.endorsements.apitest.tests;

import com.plum.endorsements.apitest.base.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

@Epic("Endorsement API")
@Feature("Submit Endorsement")
@DisplayName("POST /api/v1/endorsements/{id}/submit")
class SubmitEndorsementApiTest extends BaseApiTest {

    @Test
    @DisplayName("Should submit and auto-confirm via real-time path")
    @Description("MockInsurerAdapter returns success, so endorsement transitions to CONFIRMED")
    void shouldSubmitAndConfirm_ViaRealTimePath() {
        String id = createEndorsementAndGetId("ADD", new BigDecimal("1000.00"));

        // Submit to insurer
        given()
                .when()
                .post("/api/v1/endorsements/{id}/submit", id)
                .then()
                .statusCode(202);

        // Verify status is now CONFIRMED (MockInsurerAdapter auto-confirms)
        given()
                .when()
                .get("/api/v1/endorsements/{id}", id)
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"))
                .body("insurerReference", startsWith("INS-RT-"));
    }

    @Test
    @DisplayName("Should return 404 when submitting non-existent endorsement")
    @Description("POST submit with a non-existent UUID returns 404")
    void shouldReturn404_WhenEndorsementNotFound() {
        given()
                .when()
                .post("/api/v1/endorsements/{id}/submit", UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("title", equalTo("Endorsement Not Found"));
    }

    @Test
    @DisplayName("Should return 400 when endorsement is in invalid state for submission")
    @Description("Submitting an already CONFIRMED endorsement should fail with 400")
    void shouldReturn400_WhenEndorsementInInvalidState() {
        String id = createEndorsementAndGetId("ADD", new BigDecimal("1000.00"));

        // Submit once (auto-confirms)
        given().post("/api/v1/endorsements/{id}/submit", id)
                .then().statusCode(202);

        // Try to submit again (already CONFIRMED)
        given()
                .when()
                .post("/api/v1/endorsements/{id}/submit", id)
                .then()
                .statusCode(400)
                .body("title", equalTo("Invalid Operation"));
    }
}
