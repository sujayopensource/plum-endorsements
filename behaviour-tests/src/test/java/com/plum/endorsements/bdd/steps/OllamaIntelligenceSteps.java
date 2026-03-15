package com.plum.endorsements.bdd.steps;

import com.plum.endorsements.bdd.support.TestContext;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for Ollama intelligence graceful degradation scenarios.
 * Most steps are reused from ErrorResolutionSteps. This class adds
 * any Ollama-specific assertions if needed.
 */
public class OllamaIntelligenceSteps {

    @Autowired
    private TestContext context;

    @Then("the resolution should be produced by the rule-based adapter")
    public void theResolutionShouldBeProducedByTheRuleBasedAdapter() {
        String resolution = context.getResponse().path("resolution");
        assertThat(resolution).isNotNull().isNotEmpty();
        // Rule-based adapter produces resolutions containing "pattern" keywords
        assertThat(resolution).containsAnyOf("pattern", "matched", "format", "Manual review");
    }
}
