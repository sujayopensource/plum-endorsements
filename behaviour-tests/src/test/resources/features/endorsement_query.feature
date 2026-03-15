@allure.label.epic:Endorsement_BDD
Feature: Endorsement Querying
  As an employer
  I want to retrieve and search endorsements
  So that I can track the status of coverage changes

  Background:
    Given the standard test identifiers are configured

  @query @happy-path
  Scenario: Get endorsement by ID
    Given an existing endorsement created via the API
    When I get the endorsement by its ID
    Then the response status code should be 200
    And the endorsement type should be "ADD"
    And the endorsement status should be "PROVISIONALLY_COVERED"

  @query @error
  Scenario: Return 404 for non-existent endorsement ID
    When I get an endorsement with a random UUID
    Then the response status code should be 404

  @query @list
  Scenario: List endorsements filtered by employer ID
    Given 3 endorsements exist for the standard employer
    When I list endorsements for the standard employer
    Then the response status code should be 200
    And the response should contain 3 endorsements

  @query @filter
  Scenario: Filter endorsements by status
    Given an endorsement exists with status "PROVISIONALLY_COVERED"
    And an endorsement exists with status "CONFIRMED"
    When I list endorsements with status filter "CONFIRMED"
    Then the response status code should be 200
    And the response should contain 1 endorsements
    And all returned endorsements should have status "CONFIRMED"

  @query @pagination
  Scenario: Paginate endorsement results
    Given 3 endorsements exist for the standard employer
    When I list endorsements with page 0 and size 2
    Then the response status code should be 200
    And the response should contain 2 endorsements
    And the total elements should be 3
    And the total pages should be 2

  @query @empty
  Scenario: Return empty page for employer with no endorsements
    When I list endorsements for a random employer
    Then the response status code should be 200
    And the response should contain 0 endorsements
