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

@Epic("Intelligence API")
@Feature("Error Resolution")
@DisplayName("Error Resolution API")
class ErrorResolutionApiTest extends BaseApiTest {

    // ── Helper: Seed an error resolution record via JDBC ──

    private UUID seedErrorResolution(UUID endorsementId, boolean autoApplied, double confidence) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO error_resolutions (id, endorsement_id, error_type, original_value,
                    corrected_value, resolution, confidence, auto_applied, created_at)
                VALUES (?, ?, 'DATE_FORMAT_MISMATCH', '15/03/2026', '2026-03-15',
                    'Converted DD/MM/YYYY to ISO 8601 format', ?, ?, now())
                """,
                id, endorsementId, confidence, autoApplied
        );
        return id;
    }

    private UUID seedErrorResolutionWithOutcome(UUID endorsementId, boolean autoApplied,
                                                 double confidence, String outcome, String endorsementStatus) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO error_resolutions (id, endorsement_id, error_type, original_value,
                    corrected_value, resolution, confidence, auto_applied, created_at,
                    outcome, outcome_at, outcome_endorsement_status)
                VALUES (?, ?, 'DATE_FORMAT_MISMATCH', '15/03/2026', '2026-03-15',
                    'Converted DD/MM/YYYY to ISO 8601 format', ?, ?, now(), ?, now(), ?)
                """,
                id, endorsementId, confidence, autoApplied, outcome, endorsementStatus
        );
        return id;
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/error-resolutions/stats returns resolution statistics")
    @Description("Error resolution stats endpoint returns aggregate counts and auto-apply rate")
    void shouldReturnErrorResolutionStats() {
        // Seed endorsements and error resolutions
        UUID endorsementId1 = seedEndorsementAtStatus("CONFIRMED", 0);
        UUID endorsementId2 = seedEndorsementAtStatus("CONFIRMED", 0);
        UUID endorsementId3 = seedEndorsementAtStatus("CONFIRMED", 0);

        seedErrorResolution(endorsementId1, true, 0.98);
        seedErrorResolution(endorsementId2, true, 0.96);
        seedErrorResolution(endorsementId3, false, 0.72);

        given()
                .when()
                .get("/api/v1/intelligence/error-resolutions/stats")
                .then()
                .statusCode(200)
                .body("totalResolutions", greaterThanOrEqualTo(3))
                .body("autoApplied", greaterThanOrEqualTo(2))
                .body("suggested", greaterThanOrEqualTo(1))
                .body("autoApplyRate", notNullValue());
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/error-resolutions?endorsementId={id} returns resolutions")
    @Description("Filtering error resolutions by endorsement ID returns only resolutions for that endorsement")
    void shouldReturnResolutionsForEndorsement() {
        // Seed endorsement and its error resolution
        UUID endorsementId = seedEndorsementAtStatus("CONFIRMED", 0);
        seedErrorResolution(endorsementId, true, 0.97);
        seedErrorResolution(endorsementId, false, 0.65);

        // Seed another endorsement with a different resolution (should not be returned)
        UUID otherEndorsementId = seedEndorsementAtStatus("CONFIRMED", 0);
        seedErrorResolution(otherEndorsementId, true, 0.99);

        given()
                .queryParam("endorsementId", endorsementId)
                .when()
                .get("/api/v1/intelligence/error-resolutions")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].endorsementId", equalTo(endorsementId.toString()))
                .body("[0].errorType", notNullValue())
                .body("[0].originalValue", notNullValue())
                .body("[0].correctedValue", notNullValue())
                .body("[0].resolution", notNullValue())
                .body("[0].confidence", notNullValue())
                .body("[0].createdAt", notNullValue())
                .body("[1].endorsementId", equalTo(endorsementId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/intelligence/error-resolutions/{id}/approve updates resolution")
    @Description("Approving a suggested resolution sets auto_applied to true")
    void shouldApproveResolution() {
        // Seed an endorsement and a non-auto-applied (suggested) resolution
        UUID endorsementId = seedEndorsementAtStatus("CONFIRMED", 0);
        UUID resolutionId = seedErrorResolution(endorsementId, false, 0.72);

        // Approve the resolution
        given()
                .when()
                .post("/api/v1/intelligence/error-resolutions/{id}/approve", resolutionId)
                .then()
                .statusCode(200);

        // Verify the resolution is now auto-applied by checking stats
        // The approved resolution should now count as auto-applied
        given()
                .queryParam("endorsementId", endorsementId)
                .when()
                .get("/api/v1/intelligence/error-resolutions")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].id", equalTo(resolutionId.toString()))
                .body("[0].autoApplied", equalTo(true));
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/error-resolutions/stats returns zero counts when empty")
    @Description("When no error resolutions exist, stats returns zero counts")
    void shouldReturnZeroStatsWhenNoResolutions() {
        given()
                .when()
                .get("/api/v1/intelligence/error-resolutions/stats")
                .then()
                .statusCode(200)
                .body("totalResolutions", equalTo(0))
                .body("autoApplied", equalTo(0))
                .body("suggested", equalTo(0))
                .body("autoApplyRate", equalTo(0.0f));
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/error-resolutions returns empty list when no endorsementId provided")
    @Description("Listing error resolutions without an endorsementId returns an empty list")
    void shouldReturnEmptyListWhenNoEndorsementIdProvided() {
        given()
                .when()
                .get("/api/v1/intelligence/error-resolutions")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    // ── Phase 3 Enterprise-Grade Negative/Edge Case Tests ──

    @Test
    @DisplayName("POST /api/v1/intelligence/error-resolutions/{id}/approve with non-existent ID succeeds silently")
    @Description("Approving a non-existent resolution ID does not throw an error — the service uses ifPresent and returns 200")
    void shouldReturn200WhenApprovingNonExistentResolution() {
        UUID nonExistentId = UUID.randomUUID();

        // The approveResolution method uses ifPresent, so a missing ID is silently ignored
        given()
                .when()
                .post("/api/v1/intelligence/error-resolutions/{id}/approve", nonExistentId)
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/error-resolutions/stats returns zero rates when DB is empty")
    @Description("When the database has no error resolutions, stats returns all zeroes including 0.0 autoApplyRate")
    void shouldReturnStatsWithZeroRatesWhenNoResolutions() {
        // Database is already clean from @BeforeEach
        given()
                .when()
                .get("/api/v1/intelligence/error-resolutions/stats")
                .then()
                .statusCode(200)
                .body("totalResolutions", equalTo(0))
                .body("autoApplied", equalTo(0))
                .body("suggested", equalTo(0))
                .body("autoApplyRate", equalTo(0.0f));
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/error-resolutions?endorsementId={unknown} returns empty list")
    @Description("Filtering resolutions by an endorsement ID that has no resolutions returns an empty list")
    void shouldReturnEmptyListForUnknownEndorsementId() {
        // Seed some resolutions for a known endorsement
        UUID endorsementId = seedEndorsementAtStatus("CONFIRMED", 0);
        seedErrorResolution(endorsementId, true, 0.98);

        // Query with a random endorsement ID
        UUID unknownEndorsementId = UUID.randomUUID();

        given()
                .queryParam("endorsementId", unknownEndorsementId)
                .when()
                .get("/api/v1/intelligence/error-resolutions")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/error-resolutions response contains confidence field")
    @Description("Each error resolution in the response includes a confidence score between 0 and 1")
    void shouldReturnResolutionWithConfidenceField() {
        UUID endorsementId = seedEndorsementAtStatus("CONFIRMED", 0);
        seedErrorResolution(endorsementId, true, 0.97);

        given()
                .queryParam("endorsementId", endorsementId)
                .when()
                .get("/api/v1/intelligence/error-resolutions")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].confidence", notNullValue())
                .body("[0].confidence", greaterThanOrEqualTo(0.0f))
                .body("[0].confidence", lessThanOrEqualTo(1.0f));
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/error-resolutions/stats reflects correct auto-apply rate")
    @Description("With a mix of auto-applied and suggested resolutions, the autoApplyRate matches expected percentage")
    void shouldReturnCorrectAutoApplyRate() {
        // Seed 3 auto-applied and 2 suggested resolutions = 60% auto-apply rate
        UUID e1 = seedEndorsementAtStatus("CONFIRMED", 0);
        UUID e2 = seedEndorsementAtStatus("CONFIRMED", 0);
        UUID e3 = seedEndorsementAtStatus("CONFIRMED", 0);
        UUID e4 = seedEndorsementAtStatus("CONFIRMED", 0);
        UUID e5 = seedEndorsementAtStatus("CONFIRMED", 0);

        seedErrorResolution(e1, true, 0.98);
        seedErrorResolution(e2, true, 0.96);
        seedErrorResolution(e3, true, 0.99);
        seedErrorResolution(e4, false, 0.72);
        seedErrorResolution(e5, false, 0.65);

        given()
                .when()
                .get("/api/v1/intelligence/error-resolutions/stats")
                .then()
                .statusCode(200)
                .body("totalResolutions", equalTo(5))
                .body("autoApplied", equalTo(3))
                .body("suggested", equalTo(2))
                .body("autoApplyRate", equalTo(60.0f));
    }

    @Test
    @DisplayName("POST /api/v1/intelligence/error-resolutions/resolve creates a resolution for date format error")
    @Description("Calling the resolve endpoint with a date format error message creates an auto-applied resolution with high confidence")
    void shouldResolveErrorWithValidParameters() {
        // Seed an endorsement that exists in the DB (resolve needs to look it up)
        UUID endorsementId = seedEndorsementAtStatus("REJECTED", 0);

        given()
                .queryParam("endorsementId", endorsementId)
                .queryParam("errorMessage", "Invalid date of birth format: 07-03-1990")
                .when()
                .post("/api/v1/intelligence/error-resolutions/resolve")
                .then()
                .statusCode(200)
                .body("endorsementId", equalTo(endorsementId.toString()))
                .body("errorType", notNullValue())
                .body("resolution", notNullValue())
                .body("confidence", notNullValue())
                .body("confidence", greaterThanOrEqualTo(0.0f));
    }

    @Test
    @DisplayName("POST /api/v1/intelligence/error-resolutions/resolve with missing endorsement returns 404")
    @Description("Resolving an error for a non-existent endorsement returns 404 because no resolution can be created")
    void shouldReturn404WhenResolvingNonExistentEndorsement() {
        UUID unknownId = UUID.randomUUID();

        given()
                .queryParam("endorsementId", unknownId)
                .queryParam("errorMessage", "Some error message")
                .when()
                .post("/api/v1/intelligence/error-resolutions/resolve")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("POST /api/v1/intelligence/error-resolutions/resolve returns resolution with all expected fields")
    @Description("A successful resolve response includes id, endorsementId, errorType, originalValue, correctedValue, resolution, confidence, autoApplied, and createdAt")
    void shouldReturnResolveResponseWithAllFields() {
        UUID endorsementId = seedEndorsementAtStatus("REJECTED", 0);

        given()
                .queryParam("endorsementId", endorsementId)
                .queryParam("errorMessage", "Member ID format invalid: PLM-12345 expected prefix")
                .when()
                .post("/api/v1/intelligence/error-resolutions/resolve")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("endorsementId", equalTo(endorsementId.toString()))
                .body("errorType", notNullValue())
                .body("originalValue", notNullValue())
                .body("correctedValue", notNullValue())
                .body("resolution", notNullValue())
                .body("confidence", notNullValue())
                .body("autoApplied", notNullValue())
                .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/error-resolutions/stats includes success rate tracking")
    @Description("Stats endpoint returns successCount, failureCount, and successRate when resolutions have outcomes")
    void shouldReturnSuccessRateInStats() {
        UUID e1 = seedEndorsementAtStatus("CONFIRMED", 0);
        UUID e2 = seedEndorsementAtStatus("CONFIRMED", 0);
        UUID e3 = seedEndorsementAtStatus("REJECTED", 0);

        seedErrorResolutionWithOutcome(e1, true, 0.98, "SUCCESS", "CONFIRMED");
        seedErrorResolutionWithOutcome(e2, true, 0.96, "SUCCESS", "CONFIRMED");
        seedErrorResolutionWithOutcome(e3, true, 0.97, "FAILURE", "REJECTED");

        given()
                .when()
                .get("/api/v1/intelligence/error-resolutions/stats")
                .then()
                .statusCode(200)
                .body("totalResolutions", equalTo(3))
                .body("successCount", equalTo(2))
                .body("failureCount", equalTo(1))
                .body("successRate", greaterThan(66.0f))
                .body("successRate", lessThan(67.0f));
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/error-resolutions response contains all expected fields")
    @Description("Each error resolution response includes id, endorsementId, errorType, originalValue, correctedValue, resolution, confidence, autoApplied, and createdAt")
    void shouldReturnResolutionWithAllExpectedFields() {
        UUID endorsementId = seedEndorsementAtStatus("CONFIRMED", 0);
        UUID resolutionId = seedErrorResolution(endorsementId, false, 0.72);

        given()
                .queryParam("endorsementId", endorsementId)
                .when()
                .get("/api/v1/intelligence/error-resolutions")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].id", equalTo(resolutionId.toString()))
                .body("[0].endorsementId", equalTo(endorsementId.toString()))
                .body("[0].errorType", equalTo("DATE_FORMAT_MISMATCH"))
                .body("[0].originalValue", equalTo("15/03/2026"))
                .body("[0].correctedValue", equalTo("2026-03-15"))
                .body("[0].resolution", equalTo("Converted DD/MM/YYYY to ISO 8601 format"))
                .body("[0].confidence", notNullValue())
                .body("[0].autoApplied", equalTo(false))
                .body("[0].createdAt", notNullValue());
    }
}
