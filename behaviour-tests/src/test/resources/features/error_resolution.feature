@allure.label.epic:Endorsement_BDD
Feature: Automated Error Resolution
  As a platform operator
  I want common endorsement rejection errors to be automatically resolved
  So that the STP rate improves and manual intervention is minimized

  Background:
    Given the standard test identifiers are configured

  @error-resolution @auto-resolve
  Scenario: DOB format error auto-resolved with high confidence
    Given a REJECTED endorsement exists with error code "INVALID_DOB_FORMAT" and message "Date of birth 03-07-1990 must be in YYYY-MM-DD format"
    When I request error resolution suggestions for the endorsement
    Then the response status code should be 200
    And the resolution suggestion should have confidence greater than 0.8
    And the resolution suggestion category should be "DATE_FORMAT"
    And the resolution should contain a fix explanation
    And the resolution original value should be "03-07-1990"
    And the resolution corrected value should be "1990-07-03"
    And the resolution should be auto-applied

  @error-resolution @low-confidence
  Scenario: Low-confidence suggestion not auto-applied
    Given a REJECTED endorsement exists with error code "MEMBER_NOT_FOUND" and message "Insurer records could not locate the specified policy holder"
    When I request error resolution suggestions for the endorsement
    Then the response status code should be 200
    And the resolution suggestion should have confidence less than 0.5
    And the resolution should contain a fix explanation
    And the resolution should not be auto-applied

  @error-resolution @unknown
  Scenario: Unknown error returns generic suggestion
    Given a REJECTED endorsement exists with error code "UNKNOWN_ERR_999" and message "An unexpected error occurred during processing"
    When I request error resolution suggestions for the endorsement
    Then the response status code should be 200
    And the resolution suggestion category should be "UNKNOWN_ERROR"
    And the resolution should contain a fix explanation
    And the resolution suggestion should include a generic recommendation

  @error-resolution @member-id
  Scenario: Member ID format error resolved with insurer-specific prefix
    Given an endorsement "END-MID" exists with status "REJECTED" for insurer "INSURER-MID"
    And the rejection error is "Invalid member ID format" with code "MEMBER_ID_ERR"
    When the system attempts to resolve the error
    Then the resolution type should be "MEMBER_ID_FORMAT"
    And the corrected value should start with "PLM-"
    And the confidence should be at least 0.95

  @error-resolution @premium-mismatch
  Scenario: Premium mismatch error resolved with recalculation
    Given an endorsement "END-PM" exists with status "REJECTED" for insurer "INSURER-PM"
    And the endorsement has premium amount 1000.00
    And the rejection error is "Premium amount mismatch" with code "PREMIUM_ERR"
    When the system attempts to resolve the error
    Then the resolution type should be "PREMIUM_MISMATCH"
    And the corrected value should be different from the original
    And the confidence should be less than 0.95
    And the resolution should NOT be auto-applied

  @error-resolution @success-tracking
  Scenario: Error resolution stats include success tracking
    Given error resolutions exist with mixed outcomes
    When I request the error resolution stats
    Then the response status code should be 200
    And the stats should include success count
    And the stats should include failure count
    And the stats should include success rate

  @error-resolution @multi-error
  Scenario: Multiple errors on same endorsement resolved sequentially
    Given an endorsement "END-MULTI" exists with status "REJECTED" for insurer "INSURER-MULTI"
    And the rejection error is "DOB format invalid, missing required field: email" with code "MULTI_ERR"
    When the system attempts to resolve the error
    Then the resolution should address the first matching pattern
