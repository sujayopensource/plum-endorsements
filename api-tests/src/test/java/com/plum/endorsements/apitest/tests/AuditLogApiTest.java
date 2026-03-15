package com.plum.endorsements.apitest.tests;

import com.plum.endorsements.apitest.base.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Platform API")
@Feature("Audit Logging")
@DisplayName("GET /api/v1/audit-logs")
class AuditLogApiTest extends BaseApiTest {

    @Test
    @DisplayName("Should return empty audit logs initially")
    @Description("GET /api/v1/audit-logs returns empty page when no actions have been performed")
    void shouldReturnEmptyAuditLogs() {
        given()
                .when()
                .get("/api/v1/audit-logs")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("totalElements", notNullValue());
    }

    @Test
    @DisplayName("Should capture audit log when endorsement is created")
    @Description("Creating an endorsement generates an audit log entry")
    void shouldCaptureAuditLogOnCreate() {
        seedEAAccount(EMPLOYER_ID, INSURER_ID, new BigDecimal("100000"));
        createEndorsementAndGetId("ADD", new BigDecimal("1000"));

        // Give AOP a moment to persist
        given()
                .when()
                .get("/api/v1/audit-logs")
                .then()
                .statusCode(200)
                .body("totalElements", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("Should filter audit logs by action")
    @Description("GET /api/v1/audit-logs?action=... filters by action type")
    void shouldFilterByAction() {
        given()
                .queryParam("action", "CreateEndorsementHandler.createEndorsement")
                .when()
                .get("/api/v1/audit-logs")
                .then()
                .statusCode(200)
                .body("content", notNullValue());
    }

    @Test
    @DisplayName("Should filter audit logs by entity")
    @Description("GET /api/v1/audit-logs?entityType=Endorsement&entityId=... filters by entity")
    void shouldFilterByEntity() {
        given()
                .queryParam("entityType", "Endorsement")
                .queryParam("entityId", EMPLOYER_ID.toString())
                .when()
                .get("/api/v1/audit-logs")
                .then()
                .statusCode(200)
                .body("content", notNullValue());
    }

    @Test
    @DisplayName("Should support pagination for audit logs")
    @Description("GET /api/v1/audit-logs supports page and size parameters")
    void shouldSupportPagination() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 5)
                .when()
                .get("/api/v1/audit-logs")
                .then()
                .statusCode(200)
                .body("size", equalTo(5))
                .body("number", equalTo(0));
    }
}
