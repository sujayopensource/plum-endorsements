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
@Feature("Ollama Integration")
@DisplayName("Ollama Integration API (rule-based fallback)")
class OllamaIntegrationApiTest extends BaseApiTest {

    @Test
    @DisplayName("POST /api/v1/intelligence/error-resolutions/resolve works with Ollama disabled (rule-based active)")
    @Description("When endorsement.intelligence.ollama.enabled=false, the error resolution endpoint uses the SimulatedErrorResolver and returns a valid resolution")
    void shouldResolveErrorWithRuleBasedAdapterWhenOllamaDisabled() {
        UUID endorsementId = seedEndorsementAtStatus("REJECTED", 0);

        given()
                .queryParam("endorsementId", endorsementId)
                .queryParam("errorMessage", "Invalid date of birth format: 07-03-1990")
                .when()
                .post("/api/v1/intelligence/error-resolutions/resolve")
                .then()
                .statusCode(200)
                .body("endorsementId", equalTo(endorsementId.toString()))
                .body("errorType", equalTo("DATE_FORMAT"))
                .body("originalValue", notNullValue())
                .body("correctedValue", notNullValue())
                .body("resolution", containsString("date_format_mismatch"))
                .body("confidence", equalTo(0.98f))
                .body("autoApplied", equalTo(true));
    }
}
