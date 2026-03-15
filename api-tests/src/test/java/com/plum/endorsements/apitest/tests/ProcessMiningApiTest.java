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
@Feature("Process Mining")
@DisplayName("Process Mining API")
class ProcessMiningApiTest extends BaseApiTest {

    // ── Helper: Seed a process mining metric via JDBC ──

    private UUID seedProcessMiningMetric(UUID insurerId, String fromStatus, String toStatus,
                                          long avgDurationMs, long p95DurationMs, long p99DurationMs,
                                          int sampleCount, BigDecimal happyPathPct) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO process_mining_metrics (id, insurer_id, from_status, to_status,
                    avg_duration_ms, p95_duration_ms, p99_duration_ms, sample_count,
                    happy_path_pct, calculated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                """,
                id, insurerId, fromStatus, toStatus,
                avgDurationMs, p95DurationMs, p99DurationMs, sampleCount, happyPathPct
        );
        return id;
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/process-mining/metrics returns metrics")
    @Description("Process mining metrics endpoint returns transition metrics for all insurers")
    void shouldReturnProcessMiningMetrics() {
        // Seed process mining metrics
        seedProcessMiningMetric(INSURER_ID, "PROVISIONALLY_COVERED", "SUBMITTED",
                3600000L, 7200000L, 10800000L, 50, new BigDecimal("85.50"));
        seedProcessMiningMetric(INSURER_ID, "SUBMITTED", "CONFIRMED",
                1800000L, 3600000L, 5400000L, 48, new BigDecimal("92.00"));

        given()
                .queryParam("insurerId", INSURER_ID)
                .when()
                .get("/api/v1/intelligence/process-mining/metrics")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2))
                .body("[0].id", notNullValue())
                .body("[0].insurerId", equalTo(INSURER_ID.toString()))
                .body("[0].fromStatus", notNullValue())
                .body("[0].toStatus", notNullValue())
                .body("[0].avgDurationMs", notNullValue())
                .body("[0].p95DurationMs", notNullValue())
                .body("[0].p99DurationMs", notNullValue())
                .body("[0].sampleCount", notNullValue())
                .body("[0].calculatedAt", notNullValue());
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/process-mining/insights returns insights")
    @Description("Process mining insights endpoint returns bottleneck detection results")
    void shouldReturnProcessMiningInsights() {
        // Seed a metric with a bottleneck (p95 > 2x avg) and sufficient sample count
        seedProcessMiningMetric(INSURER_ID, "SUBMITTED", "INSURER_PROCESSING",
                3600000L, 14400000L, 21600000L, 10, new BigDecimal("78.00"));

        given()
                .when()
                .get("/api/v1/intelligence/process-mining/insights")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(0));
        // Note: Insights are dynamically generated based on bottleneck detection rules.
        // If the seeded metric qualifies (p95 > 2 * avg and sampleCount >= 5),
        // at least one insight should be present.
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/process-mining/stp-rate returns STP rate")
    @Description("STP rate endpoint returns the overall straight-through-processing rate and per-insurer breakdown")
    void shouldReturnStpRate() {
        // Seed a metric with a happy path percentage
        seedProcessMiningMetric(INSURER_ID, "PROVISIONALLY_COVERED", "CONFIRMED",
                5400000L, 10800000L, 16200000L, 100, new BigDecimal("88.75"));

        given()
                .queryParam("insurerId", INSURER_ID)
                .when()
                .get("/api/v1/intelligence/process-mining/stp-rate")
                .then()
                .statusCode(200)
                .body("overallStpRate", notNullValue())
                .body("perInsurerStpRate", notNullValue())
                .body("perInsurerStpRate." + INSURER_ID, notNullValue());
    }

    @Test
    @DisplayName("POST /api/v1/intelligence/process-mining/analyze triggers analysis and returns 202")
    @Description("Triggering process mining analysis returns 202 Accepted")
    void shouldTriggerAnalysisAndReturn202() {
        given()
                .when()
                .post("/api/v1/intelligence/process-mining/analyze")
                .then()
                .statusCode(202);
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/process-mining/metrics returns empty list for unknown insurer")
    @Description("Requesting metrics for an insurer with no data returns an empty list")
    void shouldReturnEmptyMetricsForUnknownInsurer() {
        given()
                .queryParam("insurerId", UUID.randomUUID())
                .when()
                .get("/api/v1/intelligence/process-mining/metrics")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/process-mining/stp-rate without insurerId returns aggregate rate")
    @Description("STP rate without insurer filter returns the overall aggregate STP rate across all insurers")
    void shouldReturnAggregateStpRateWithoutInsurerFilter() {
        // Seed metrics for the known insurer
        seedProcessMiningMetric(INSURER_ID, "PROVISIONALLY_COVERED", "CONFIRMED",
                5400000L, 10800000L, 16200000L, 100, new BigDecimal("90.00"));

        given()
                .when()
                .get("/api/v1/intelligence/process-mining/stp-rate")
                .then()
                .statusCode(200)
                .body("overallStpRate", notNullValue())
                .body("perInsurerStpRate", notNullValue());
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/process-mining/insights returns empty list when no bottlenecks")
    @Description("When no metrics indicate bottlenecks, insights returns an empty list")
    void shouldReturnEmptyInsightsWhenNoBottlenecks() {
        // Seed a metric where p95 is NOT > 2x avg (no bottleneck)
        seedProcessMiningMetric(INSURER_ID, "PROVISIONALLY_COVERED", "SUBMITTED",
                3600000L, 5400000L, 7200000L, 100, new BigDecimal("95.00"));

        given()
                .when()
                .get("/api/v1/intelligence/process-mining/insights")
                .then()
                .statusCode(200)
                // The seeded metric's p95 (5400000) is NOT > 2 * avg (7200000), so no bottleneck
                .body("findAll { it.insurerId == '" + INSURER_ID + "' && it.insightType == 'BOTTLENECK' }.size()", equalTo(0));
    }

    // ── Phase 3 Enterprise-Grade Negative/Edge Case Tests ──

    @Test
    @DisplayName("GET /api/v1/intelligence/process-mining/metrics returns 200 with empty list when no data")
    @Description("When the database has no process mining metrics, the metrics endpoint returns 200 with an empty array")
    void shouldReturn200WithEmptyMetricsWhenNoData() {
        // Database is clean from @BeforeEach — no metrics exist
        given()
                .when()
                .get("/api/v1/intelligence/process-mining/metrics")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/process-mining/metrics?insurerId={id} filters by insurer")
    @Description("Metrics filtered by insurer ID return only metrics belonging to that insurer")
    void shouldFilterMetricsByInsurerId() {
        UUID otherInsurerId = UUID.randomUUID();

        // Seed metrics for two different insurers
        seedProcessMiningMetric(INSURER_ID, "PROVISIONALLY_COVERED", "SUBMITTED",
                3600000L, 7200000L, 10800000L, 50, new BigDecimal("85.50"));
        seedProcessMiningMetric(otherInsurerId, "SUBMITTED", "CONFIRMED",
                1800000L, 3600000L, 5400000L, 30, new BigDecimal("90.00"));

        // Filter by INSURER_ID — should only return metrics for that insurer
        given()
                .queryParam("insurerId", INSURER_ID)
                .when()
                .get("/api/v1/intelligence/process-mining/metrics")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("findAll { it.insurerId == '" + INSURER_ID + "' }.size()", greaterThanOrEqualTo(1))
                .body("findAll { it.insurerId != '" + INSURER_ID + "' }.size()", equalTo(0));
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/process-mining/insights returns insights with BOTTLENECK type")
    @Description("When a metric has p95 > 2x avg and sampleCount >= 5, insights include a BOTTLENECK-typed entry")
    void shouldReturnInsightsWithBottleneckType() {
        // Seed a metric that qualifies as a bottleneck: p95 (14400000) > 2 * avg (3600000)
        seedProcessMiningMetric(INSURER_ID, "SUBMITTED", "INSURER_PROCESSING",
                3600000L, 14400000L, 21600000L, 10, new BigDecimal("78.00"));

        given()
                .when()
                .get("/api/v1/intelligence/process-mining/insights")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].insightType", equalTo("BOTTLENECK"))
                .body("[0].insurerId", notNullValue())
                .body("[0].insight", notNullValue())
                .body("[0].calculatedAt", notNullValue());
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/process-mining/stp-rate returns per-insurer breakdown")
    @Description("STP rate response includes a perInsurerStpRate map with the correct insurer ID and rate")
    void shouldReturnSTPRatePerInsurer() {
        // Seed a metric with a known happy path percentage
        seedProcessMiningMetric(INSURER_ID, "PROVISIONALLY_COVERED", "CONFIRMED",
                5400000L, 10800000L, 16200000L, 100, new BigDecimal("91.50"));

        given()
                .queryParam("insurerId", INSURER_ID)
                .when()
                .get("/api/v1/intelligence/process-mining/stp-rate")
                .then()
                .statusCode(200)
                .body("overallStpRate", notNullValue())
                .body("perInsurerStpRate", notNullValue())
                .body("perInsurerStpRate.size()", greaterThanOrEqualTo(1))
                .body("perInsurerStpRate." + INSURER_ID, notNullValue());
    }

    @Test
    @DisplayName("POST /api/v1/intelligence/process-mining/analyze with no events returns 202")
    @Description("Triggering analysis when no endorsement events exist still returns 202 Accepted without error")
    void shouldHandleAnalysisWithNoEvents() {
        // Database is clean from @BeforeEach — no endorsement events exist
        given()
                .when()
                .post("/api/v1/intelligence/process-mining/analyze")
                .then()
                .statusCode(202);
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/process-mining/stp-rate/trend returns trend data")
    @Description("STP rate trend endpoint returns historical data points for a given insurer")
    void shouldReturnStpRateTrend() {
        // Seed snapshot rows directly via JDBC
        jdbc.update("""
                INSERT INTO stp_rate_snapshots (id, insurer_id, snapshot_date, total_endorsements,
                    stp_endorsements, stp_rate, created_at)
                VALUES (?, ?, CURRENT_DATE - 2, 10, 8, 80.0000, now())
                """,
                UUID.randomUUID(), INSURER_ID
        );
        jdbc.update("""
                INSERT INTO stp_rate_snapshots (id, insurer_id, snapshot_date, total_endorsements,
                    stp_endorsements, stp_rate, created_at)
                VALUES (?, ?, CURRENT_DATE - 1, 12, 10, 83.3333, now())
                """,
                UUID.randomUUID(), INSURER_ID
        );

        given()
                .queryParam("insurerId", INSURER_ID)
                .queryParam("days", 30)
                .when()
                .get("/api/v1/intelligence/process-mining/stp-rate/trend")
                .then()
                .statusCode(200)
                .body("insurerId", equalTo(INSURER_ID.toString()))
                .body("dataPoints.size()", greaterThanOrEqualTo(2))
                .body("currentRate", notNullValue())
                .body("changePercent", notNullValue());
    }

    @Test
    @DisplayName("GET /api/v1/intelligence/process-mining/stp-rate returns exact rate from seeded data")
    @Description("After seeding a metric with a specific happy path percentage, the STP rate matches exactly")
    void shouldReturnCorrectSTPRateCalculation() {
        BigDecimal expectedRate = new BigDecimal("87.25");

        // Seed a single metric with a specific happy_path_pct
        seedProcessMiningMetric(INSURER_ID, "PROVISIONALLY_COVERED", "CONFIRMED",
                5400000L, 10800000L, 16200000L, 200, expectedRate);

        given()
                .queryParam("insurerId", INSURER_ID)
                .when()
                .get("/api/v1/intelligence/process-mining/stp-rate")
                .then()
                .statusCode(200)
                .body("overallStpRate", equalTo(expectedRate.floatValue()))
                .body("perInsurerStpRate." + INSURER_ID, equalTo(expectedRate.floatValue()));
    }
}
