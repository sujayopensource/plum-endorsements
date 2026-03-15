package com.plum.endorsements.apitest.tests;

import com.plum.endorsements.apitest.base.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Epic("Intelligence API")
@Feature("Cross-Insurer Benchmarking")
@DisplayName("GET /api/v1/intelligence/benchmarks")
class InsurerBenchmarkApiTest extends BaseApiTest {

    @Test
    @DisplayName("Should return benchmarks for all active insurers")
    @Description("Returns benchmark data for all configured insurers including STP rate and processing times")
    void shouldReturnBenchmarksForAllInsurers() {
        given()
                .when()
                .get("/api/v1/intelligence/benchmarks")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(4))
                .body("insurerName", everyItem(notNullValue()))
                .body("insurerCode", everyItem(notNullValue()))
                .body("stpRate", everyItem(notNullValue()))
                .body("calculatedAt", everyItem(notNullValue()));
    }

    @Test
    @DisplayName("Should include processing time metrics in benchmarks")
    @Description("Each benchmark entry includes avg, p95, and p99 processing times")
    void shouldIncludeProcessingTimeMetrics() {
        given()
                .when()
                .get("/api/v1/intelligence/benchmarks")
                .then()
                .statusCode(200)
                .body("[0].avgProcessingMs", notNullValue())
                .body("[0].p95ProcessingMs", notNullValue())
                .body("[0].p99ProcessingMs", notNullValue())
                .body("[0].totalSamples", notNullValue());
    }

    @Test
    @DisplayName("Benchmarks are sorted by STP rate descending")
    @Description("Best performing insurers appear first in the list")
    void shouldBeSortedByStpRate() {
        var response = given()
                .when()
                .get("/api/v1/intelligence/benchmarks")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        List<Float> stpRates = response.getList("stpRate", Float.class);
        for (int i = 0; i < stpRates.size() - 1; i++) {
            assertThat(stpRates.get(i)).isGreaterThanOrEqualTo(stpRates.get(i + 1));
        }
    }
}
