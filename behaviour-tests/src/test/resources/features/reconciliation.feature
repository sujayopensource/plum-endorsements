@allure.label.epic:Endorsement_BDD
Feature: Reconciliation
  As a platform operator
  I want to trigger and track reconciliation runs against insurers
  So that I can verify endorsement statuses match insurer records

  Background:
    Given the standard test identifiers are configured

  @reconciliation @trigger
  Scenario: Trigger reconciliation for an insurer
    When I trigger reconciliation for the mock insurer
    Then the response status code should be 200
    And the response body should contain field "id"
    And the response body should contain field "insurerId"
    And the response body should contain field "status"
    And the response body should contain field "totalChecked"

  @reconciliation @runs
  Scenario: List reconciliation runs for an insurer
    Given I trigger reconciliation for the mock insurer
    When I list reconciliation runs for the mock insurer
    Then the response status code should be 200
    And the response body list size should be at least 1

  @reconciliation @items
  Scenario: Get reconciliation run items
    Given a CONFIRMED endorsement exists for the mock insurer
    And I trigger reconciliation for the mock insurer
    When I get the reconciliation run items for the first run
    Then the response status code should be 200

  @reconciliation @empty
  Scenario: Empty runs for unknown insurer
    When I list reconciliation runs for a random insurer
    Then the response status code should be 200
    And the response body list should be empty
