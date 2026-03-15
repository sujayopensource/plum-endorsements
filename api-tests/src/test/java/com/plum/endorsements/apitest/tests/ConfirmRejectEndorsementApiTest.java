package com.plum.endorsements.apitest.tests;

import com.plum.endorsements.apitest.base.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Epic("Endorsement API")
@Feature("Confirm & Reject Endorsement")
@DisplayName("Confirm and Reject Endpoints")
class ConfirmRejectEndorsementApiTest extends BaseApiTest {

    @Nested
    @DisplayName("POST /api/v1/endorsements/{id}/confirm")
    class ConfirmTests {

        @Test
        @DisplayName("Should confirm endorsement and set insurer reference")
        @Description("Confirming an endorsement in INSURER_PROCESSING state transitions it to CONFIRMED")
        void shouldConfirmEndorsement_SetsStatusAndInsurerReference() {
            UUID id = seedEndorsementAtStatus("INSURER_PROCESSING", 0);

            given()
                    .queryParam("insurerReference", "INS-REF-2026-001")
                    .when()
                    .post("/api/v1/endorsements/{id}/confirm", id)
                    .then()
                    .statusCode(200);

            given()
                    .when()
                    .get("/api/v1/endorsements/{id}", id)
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("CONFIRMED"))
                    .body("insurerReference", equalTo("INS-REF-2026-001"));
        }

        @Test
        @DisplayName("Should return 404 when confirming non-existent endorsement")
        void shouldReturn404_WhenConfirmingNonexistentEndorsement() {
            given()
                    .queryParam("insurerReference", "INS-REF-001")
                    .when()
                    .post("/api/v1/endorsements/{id}/confirm", UUID.randomUUID())
                    .then()
                    .statusCode(404)
                    .body("title", equalTo("Endorsement Not Found"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/endorsements/{id}/reject")
    class RejectTests {

        @Test
        @DisplayName("Should set retry pending when retry is available")
        @Description("Rejecting an endorsement with retryCount < 3 sets status to RETRY_PENDING")
        void shouldRejectEndorsement_SetsRetryPending_WhenRetryAvailable() {
            UUID id = seedEndorsementAtStatus("REJECTED", 0);

            given()
                    .queryParam("reason", "Employee data mismatch")
                    .when()
                    .post("/api/v1/endorsements/{id}/reject", id)
                    .then()
                    .statusCode(200);

            given()
                    .when()
                    .get("/api/v1/endorsements/{id}", id)
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("RETRY_PENDING"))
                    .body("retryCount", equalTo(1))
                    .body("failureReason", equalTo("Employee data mismatch"));
        }

        @Test
        @DisplayName("Should set failed permanent when max retries exhausted")
        @Description("Rejecting an endorsement with retryCount >= 3 sets status to FAILED_PERMANENT")
        void shouldRejectEndorsement_SetsFailedPermanent_WhenMaxRetriesExhausted() {
            UUID id = seedEndorsementAtStatus("REJECTED", 3);

            given()
                    .queryParam("reason", "Final rejection")
                    .when()
                    .post("/api/v1/endorsements/{id}/reject", id)
                    .then()
                    .statusCode(200);

            given()
                    .when()
                    .get("/api/v1/endorsements/{id}", id)
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("FAILED_PERMANENT"))
                    .body("failureReason", equalTo("Final rejection"));
        }

        @Test
        @DisplayName("Should return 404 when rejecting non-existent endorsement")
        void shouldReturn404_WhenRejectingNonexistentEndorsement() {
            given()
                    .queryParam("reason", "Some reason")
                    .when()
                    .post("/api/v1/endorsements/{id}/reject", UUID.randomUUID())
                    .then()
                    .statusCode(404)
                    .body("title", equalTo("Endorsement Not Found"));
        }
    }
}
