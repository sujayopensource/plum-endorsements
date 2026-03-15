@allure.label.epic:Endorsement_BDD
Feature: Outstanding Endorsement Items
  As an employer
  I want to view all outstanding (non-terminal) endorsements
  So that I can track pending coverage changes

  Background:
    Given the standard test identifiers are configured

  @outstanding @happy-path
  Scenario: Outstanding items returns only non-terminal endorsements
    Given an endorsement exists with status "CREATED"
    And an endorsement exists with status "VALIDATED"
    And an endorsement exists with status "PROVISIONALLY_COVERED"
    And an endorsement exists with status "CONFIRMED"
    And an endorsement exists with status "REJECTED"
    When I list outstanding items for the standard employer
    Then the response status code should be 200
    And the outstanding items count should be 3

  @outstanding @batch-statuses
  Scenario: Outstanding items includes batch-related statuses
    Given an endorsement exists with status "QUEUED_FOR_BATCH"
    And an endorsement exists with status "BATCH_SUBMITTED"
    And an endorsement exists with status "INSURER_PROCESSING"
    When I list outstanding items for the standard employer
    Then the response status code should be 200
    And the outstanding items count should be 3

  @outstanding @empty
  Scenario: Outstanding items returns empty when only terminal endorsements exist
    Given an endorsement exists with status "CONFIRMED"
    And an endorsement exists with status "REJECTED"
    When I list outstanding items for the standard employer
    Then the response status code should be 200
    And the outstanding items count should be 0

  @outstanding @pagination
  Scenario: Outstanding items supports pagination
    Given 5 endorsements exist with status "CREATED"
    When I list outstanding items with page 0 and size 2
    Then the response status code should be 200
    And the outstanding items count should be 2
    And the total elements should be 5
    And the total pages should be 3
