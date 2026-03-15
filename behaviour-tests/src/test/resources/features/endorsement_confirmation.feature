@allure.label.epic:Endorsement_BDD
Feature: Endorsement Confirmation and Rejection
  As an insurer
  I want to confirm or reject endorsements
  So that coverage changes are finalized or retried

  Background:
    Given the standard test identifiers are configured

  @confirmation @happy-path
  Scenario: Confirm endorsement sets CONFIRMED status and insurer reference
    Given an existing endorsement in "INSURER_PROCESSING" status
    When I confirm the endorsement with insurer reference "INS-REF-001"
    Then the response status code should be 200
    And the endorsement should be in "CONFIRMED" status
    And the endorsement insurer reference should be "INS-REF-001"

  @confirmation @error
  Scenario: Return 404 when confirming non-existent endorsement
    When I confirm a non-existent endorsement with insurer reference "INS-REF-001"
    Then the response status code should be 404

  @rejection @retry
  Scenario: Reject endorsement with retries available sets RETRY_PENDING
    Given an existing endorsement in "REJECTED" status with retry count 0
    When I reject the endorsement with reason "Document mismatch"
    Then the response status code should be 200
    And the endorsement should be in "RETRY_PENDING" status
    And the endorsement retry count should be 1
    And the endorsement failure reason should be "Document mismatch"

  @rejection @terminal
  Scenario: Reject endorsement with max retries exhausted sets FAILED_PERMANENT
    Given an existing endorsement in "REJECTED" status with retry count 3
    When I reject the endorsement with reason "Permanent failure"
    Then the response status code should be 200
    And the endorsement should be in "FAILED_PERMANENT" status

  @rejection @error
  Scenario: Return 404 when rejecting non-existent endorsement
    When I reject a non-existent endorsement with reason "Not found"
    Then the response status code should be 404
