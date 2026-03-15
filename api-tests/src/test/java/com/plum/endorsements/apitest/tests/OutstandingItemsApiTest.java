package com.plum.endorsements.apitest.tests;

import com.plum.endorsements.apitest.base.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Endorsement API")
@Feature("Outstanding Items")
@DisplayName("GET /api/v1/endorsements/employers/{employerId}/outstanding")
class OutstandingItemsApiTest extends BaseApiTest {

    @Test
    @DisplayName("Should return outstanding endorsements excluding terminal statuses")
    @Description("Outstanding items endpoint returns only non-terminal endorsements (CREATED, VALIDATED, etc.)")
    void shouldReturnOutstandingEndorsements() {
        // Seed endorsements at various statuses
        seedEndorsementAtStatus("CREATED", 0);
        seedEndorsementAtStatus("VALIDATED", 0);
        seedEndorsementAtStatus("PROVISIONALLY_COVERED", 0);
        seedEndorsementAtStatus("SUBMITTED_REALTIME", 0);
        // Terminal statuses — should be excluded
        seedEndorsementAtStatus("CONFIRMED", 0);
        seedEndorsementAtStatus("REJECTED", 0);

        given()
                .when()
                .get("/api/v1/endorsements/employers/{employerId}/outstanding", EMPLOYER_ID)
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(4))
                .body("totalElements", equalTo(4))
                .body("content.status", everyItem(not(equalTo("CONFIRMED"))))
                .body("content.status", everyItem(not(equalTo("REJECTED"))));
    }

    @Test
    @DisplayName("Should include QUEUED_FOR_BATCH and BATCH_SUBMITTED as outstanding")
    @Description("Batch-related non-terminal statuses are included in outstanding items")
    void shouldIncludeBatchRelatedStatuses() {
        seedEndorsementAtStatus("QUEUED_FOR_BATCH", 0);
        seedEndorsementAtStatus("BATCH_SUBMITTED", 0);
        seedEndorsementAtStatus("INSURER_PROCESSING", 0);
        seedEndorsementAtStatus("RETRY_PENDING", 1);

        given()
                .when()
                .get("/api/v1/endorsements/employers/{employerId}/outstanding", EMPLOYER_ID)
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(4))
                .body("totalElements", equalTo(4));
    }

    @Test
    @DisplayName("Should return empty page when no outstanding items exist")
    @Description("Returns empty page for an employer with only terminal endorsements")
    void shouldReturnEmptyPage_WhenOnlyTerminalEndorsements() {
        seedEndorsementAtStatus("CONFIRMED", 0);
        seedEndorsementAtStatus("REJECTED", 0);

        given()
                .when()
                .get("/api/v1/endorsements/employers/{employerId}/outstanding", EMPLOYER_ID)
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0))
                .body("totalElements", equalTo(0));
    }

    @Test
    @DisplayName("Should return empty page for employer with no endorsements")
    @Description("Returns empty page for an employer that has never created endorsements")
    void shouldReturnEmptyPage_WhenNoEndorsementsExist() {
        given()
                .when()
                .get("/api/v1/endorsements/employers/{employerId}/outstanding", UUID.randomUUID())
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0))
                .body("totalElements", equalTo(0));
    }

    @Test
    @DisplayName("Should paginate outstanding items correctly")
    @Description("Outstanding items endpoint respects page and size parameters")
    void shouldPaginateOutstandingItems() {
        // Seed 5 outstanding endorsements
        for (int i = 0; i < 5; i++) {
            seedEndorsementAtStatus("CREATED", 0);
        }

        given()
                .queryParam("page", 0)
                .queryParam("size", 2)
                .when()
                .get("/api/v1/endorsements/employers/{employerId}/outstanding", EMPLOYER_ID)
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("totalElements", equalTo(5))
                .body("totalPages", equalTo(3))
                .body("number", equalTo(0));

        // Last page
        given()
                .queryParam("page", 2)
                .queryParam("size", 2)
                .when()
                .get("/api/v1/endorsements/employers/{employerId}/outstanding", EMPLOYER_ID)
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("number", equalTo(2));
    }

    @Test
    @DisplayName("Should exclude FAILED_PERMANENT from outstanding items")
    @Description("FAILED_PERMANENT is a terminal status and should not appear in outstanding items")
    void shouldExcludeFailedPermanent() {
        seedEndorsementAtStatus("FAILED_PERMANENT", 3);
        seedEndorsementAtStatus("CREATED", 0);

        given()
                .when()
                .get("/api/v1/endorsements/employers/{employerId}/outstanding", EMPLOYER_ID)
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].status", equalTo("CREATED"));
    }
}
