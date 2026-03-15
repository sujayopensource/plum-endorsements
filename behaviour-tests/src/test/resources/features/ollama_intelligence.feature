@allure.label.epic:Endorsement_BDD
Feature: Ollama Intelligence Graceful Degradation
  As a platform operator
  I want the intelligence layer to work with or without Ollama
  So that the system degrades gracefully when LLM is unavailable

  Background:
    Given the standard test identifiers are configured

  @ollama @rule-based-fallback
  Scenario: Error resolution works with rule-based fallback when Ollama disabled
    Given a REJECTED endorsement exists with error code "INVALID_DOB_FORMAT" and message "Date of birth 03-07-1990 must be in YYYY-MM-DD format"
    When I request error resolution suggestions for the endorsement
    Then the response status code should be 200
    And the resolution suggestion category should be "DATE_FORMAT"
    And the resolution suggestion should have confidence greater than 0.9
    And the resolution should contain a fix explanation

  @ollama @anomaly-scoring
  Scenario: Anomaly detection scoring works without Ollama
    Given a REJECTED endorsement exists with error code "UNKNOWN_ERR" and message "Some unrecognized error occurred"
    When I request error resolution suggestions for the endorsement
    Then the response status code should be 200
    And the resolution suggestion category should be "UNKNOWN_ERROR"
    And the resolution should contain a fix explanation

  @ollama @graceful-degradation
  Scenario: Intelligence layer gracefully degrades with member ID errors
    Given an endorsement "END-OLLAMA" exists with status "REJECTED" for insurer "INSURER-OLLAMA"
    And the rejection error is "Invalid member ID format" with code "MEMBER_ID_ERR"
    When the system attempts to resolve the error
    Then the resolution type should be "MEMBER_ID_FORMAT"
    And the corrected value should start with "PLM-"
    And the confidence should be at least 0.95
