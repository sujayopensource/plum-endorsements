@allure.label.epic:Endorsement_BDD
Feature: End-to-End Endorsement Lifecycle
  As an employer
  I want to complete the full endorsement lifecycle
  So that employee coverage changes are fully processed

  Background:
    Given the standard test identifiers are configured

  @lifecycle @e2e
  Scenario: Complete lifecycle create -> submit -> confirm with coverage and balance
    Given an EA account exists with a balance of 50000.00
    # Step 1: Create endorsement
    When I create an "ADD" endorsement with premium 1500.00
    Then the response status code should be 201
    And the endorsement status should be "PROVISIONALLY_COVERED"
    # Step 2: Verify provisional coverage
    When I get the provisional coverage for the endorsement
    Then the response status code should be 200
    And the coverage type should be "PROVISIONAL"
    # Step 3: Verify EA reservation
    When I get the EA account for the standard employer and insurer
    Then the EA account reserved amount should be 1500.00
    And the EA account available balance should be 48500.00
    # Step 4: Submit to insurer (auto-confirms via mock)
    When I submit the endorsement to the insurer
    Then the response status code should be 202
    # Step 5: Verify final CONFIRMED status
    When I get the endorsement by its ID
    Then the endorsement should be in "CONFIRMED" status
    And the endorsement should have an insurer reference starting with "INS-RT-"
    # Step 6: Verify coverage is now CONFIRMED
    When I get the provisional coverage for the endorsement
    Then the coverage type should be "CONFIRMED"
    And the coverage should have a non-null confirmed date

  @lifecycle @field-verification
  Scenario: Verify all response fields match request values
    When I create an endorsement with full details
    And I get the endorsement by its ID
    Then the response status code should be 200
    And all endorsement fields should match the original request
