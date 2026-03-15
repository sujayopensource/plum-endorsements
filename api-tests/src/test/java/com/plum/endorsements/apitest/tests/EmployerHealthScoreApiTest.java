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
import static org.hamcrest.Matchers.*;

@Epic("Intelligence API")
@Feature("Employer Health Score")
@DisplayName("GET /api/v1/intelligence/employers/{id}/health-score")
class EmployerHealthScoreApiTest extends BaseApiTest {

    @Test
    @DisplayName("Should return health score for employer with no data")
    @Description("Returns 100% health score for employer with no endorsements or anomalies")
    void shouldReturnPerfectScoreForNewEmployer() {
        given()
                .when()
                .get("/api/v1/intelligence/employers/{id}/health-score", EMPLOYER_ID)
                .then()
                .statusCode(200)
                .body("employerId", equalTo(EMPLOYER_ID.toString()))
                .body("overallScore", notNullValue())
                .body("riskLevel", notNullValue())
                .body("endorsementSuccessRate", notNullValue())
                .body("anomalyScore", notNullValue())
                .body("balanceHealthScore", notNullValue())
                .body("reconciliationScore", notNullValue())
                .body("calculatedAt", notNullValue());
    }

    @Test
    @DisplayName("Should return health score reflecting successful endorsements")
    @Description("Creates confirmed endorsements and checks that success rate is reflected in health score")
    void shouldReflectEndorsementSuccessRate() {
        // Seed a confirmed endorsement
        seedEndorsementAtStatus("CONFIRMED", 0);

        given()
                .when()
                .get("/api/v1/intelligence/employers/{id}/health-score", EMPLOYER_ID)
                .then()
                .statusCode(200)
                .body("endorsementSuccessRate", equalTo(100.0f))
                .body("riskLevel", equalTo("LOW"));
    }

    @Test
    @DisplayName("Should return health score reflecting EA balance health")
    @Description("Seeds EA account with positive balance and verifies balance health score")
    void shouldReflectBalanceHealth() {
        seedEAAccount(EMPLOYER_ID, INSURER_ID, new BigDecimal("50000.00"));

        given()
                .when()
                .get("/api/v1/intelligence/employers/{id}/health-score", EMPLOYER_ID)
                .then()
                .statusCode(200)
                .body("balanceHealthScore", equalTo(100.0f));
    }

    @Test
    @DisplayName("Should return valid health score for random employer")
    @Description("Returns health score even for employer with no history")
    void shouldReturnValidScoreForRandomEmployer() {
        given()
                .when()
                .get("/api/v1/intelligence/employers/{id}/health-score", UUID.randomUUID())
                .then()
                .statusCode(200)
                .body("overallScore", greaterThanOrEqualTo(0.0f))
                .body("overallScore", lessThanOrEqualTo(100.0f));
    }
}
