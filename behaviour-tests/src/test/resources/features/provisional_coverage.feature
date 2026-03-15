@allure.label.epic:Endorsement_BDD
Feature: Provisional Coverage
  As an employee
  I want to have provisional coverage while my endorsement is being processed
  So that I am covered during the waiting period

  Background:
    Given the standard test identifiers are configured

  @coverage @happy-path
  Scenario: Return provisional coverage for ADD endorsement
    Given I create an "ADD" endorsement with premium 1200.00
    When I get the provisional coverage for the endorsement
    Then the response status code should be 200
    And the coverage type should be "PROVISIONAL"
    And the coverage endorsement ID should match

  @coverage @no-coverage
  Scenario: Return 404 for DELETE endorsement coverage
    Given I create a "DELETE" endorsement without premium
    When I get the provisional coverage for the endorsement
    Then the response status code should be 404

  @coverage @confirmation
  Scenario: Confirm provisional coverage after endorsement submission
    Given an EA account exists with a balance of 50000.00
    And I create an "ADD" endorsement with premium 1200.00
    When I submit the endorsement to the insurer
    And I get the provisional coverage for the endorsement
    Then the response status code should be 200
    And the coverage type should be "CONFIRMED"
    And the coverage should have a non-null confirmed date
