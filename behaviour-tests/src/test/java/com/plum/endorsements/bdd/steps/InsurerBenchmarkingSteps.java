package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class InsurerBenchmarkingSteps {

    @Autowired
    private TestContext testContext;

    @When("I request cross-insurer benchmarks")
    public void iRequestBenchmarks() {
        var response = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/intelligence/benchmarks");
        testContext.setResponse(response);
    }

    @Then("the benchmarks list should contain at least {int} entries")
    public void benchmarksListShouldContainAtLeast(int count) {
        List<?> list = testContext.getResponse().jsonPath().getList("$");
        assertThat(list).hasSizeGreaterThanOrEqualTo(count);
    }

    @Then("each benchmark should have insurer name and code")
    public void eachBenchmarkShouldHaveNameAndCode() {
        List<String> names = testContext.getResponse().jsonPath().getList("insurerName");
        List<String> codes = testContext.getResponse().jsonPath().getList("insurerCode");
        assertThat(names).allSatisfy(n -> assertThat(n).isNotBlank());
        assertThat(codes).allSatisfy(c -> assertThat(c).isNotBlank());
    }

    @Then("each benchmark should have processing time metrics")
    public void eachBenchmarkShouldHaveMetrics() {
        List<?> avgMs = testContext.getResponse().jsonPath().getList("avgProcessingMs");
        List<?> stpRates = testContext.getResponse().jsonPath().getList("stpRate");
        assertThat(avgMs).isNotEmpty();
        assertThat(stpRates).isNotEmpty();
    }

    @Then("the benchmarks should be sorted by STP rate descending")
    public void benchmarksShouldBeSortedByStpRate() {
        List<Float> stpRates = testContext.getResponse().jsonPath().getList("stpRate", Float.class);
        for (int i = 0; i < stpRates.size() - 1; i++) {
            assertThat(stpRates.get(i)).isGreaterThanOrEqualTo(stpRates.get(i + 1));
        }
    }
}
