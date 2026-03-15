package com.plum.endorsements.bdd.support;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared state between step definition classes within a single scenario.
 * Scoped per Cucumber scenario — automatically reset between scenarios.
 */
@Component
@ScenarioScope
public class TestContext {

    private Response response;
    private String endorsementId;
    private Map<String, Object> requestPayload;
    private final Map<String, String> storedValues = new HashMap<>();

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public String getEndorsementId() {
        return endorsementId;
    }

    public void setEndorsementId(String endorsementId) {
        this.endorsementId = endorsementId;
    }

    public Map<String, Object> getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(Map<String, Object> requestPayload) {
        this.requestPayload = requestPayload;
    }

    public void store(String key, String value) {
        storedValues.put(key, value);
    }

    public String retrieve(String key) {
        return storedValues.get(key);
    }

    public void reset() {
        response = null;
        endorsementId = null;
        requestPayload = null;
        storedValues.clear();
    }
}
