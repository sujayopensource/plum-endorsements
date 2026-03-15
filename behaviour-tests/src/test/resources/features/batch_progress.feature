@allure.label.epic:Endorsement_BDD
Feature: Batch Progress Tracking
  As an employer
  I want to see the progress of endorsement batches
  So that I can monitor batch submissions to insurers

  Background:
    Given the standard test identifiers are configured

  @batch-progress @happy-path
  Scenario: Batch progress returns batches linked to employer
    Given a batch exists with endorsements for the standard employer
    When I list batch progress for the standard employer
    Then the response status code should be 200
    And the batch progress count should be 1
    And the first batch should have status "SUBMITTED"

  @batch-progress @empty
  Scenario: Batch progress returns empty when no batches exist
    When I list batch progress for the standard employer
    Then the response status code should be 200
    And the batch progress count should be 0

  @batch-progress @pagination
  Scenario: Batch progress supports pagination
    Given 3 batches exist with endorsements for the standard employer
    When I list batch progress with page 0 and size 2
    Then the response status code should be 200
    And the batch progress count should be 2
    And the total elements should be 3
