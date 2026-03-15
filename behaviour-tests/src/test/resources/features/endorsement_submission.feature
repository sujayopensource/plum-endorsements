@allure.label.epic:Endorsement_BDD
Feature: Endorsement Submission to Insurer
  As an employer
  I want to submit endorsements to the insurer
  So that employee coverage changes are processed

  Background:
    Given the standard test identifiers are configured

  @submission @happy-path
  Scenario: Submit endorsement and auto-confirm via real-time path
    Given an existing endorsement in "PROVISIONALLY_COVERED" status
    When I submit the endorsement to the insurer
    Then the response status code should be 202
    And the endorsement should be in "CONFIRMED" status
    And the endorsement should have an insurer reference starting with "INS-RT-"

  @submission @error
  Scenario: Return 404 when submitting non-existent endorsement
    When I submit a non-existent endorsement to the insurer
    Then the response status code should be 404

  @submission @error
  Scenario: Return error when submitting already confirmed endorsement
    Given an existing endorsement in "CONFIRMED" status
    When I submit the endorsement to the insurer
    Then the response status code should be 400
