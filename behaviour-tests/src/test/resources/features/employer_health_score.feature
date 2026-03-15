@allure.label.epic:Endorsement_BDD
Feature: Employer Health Score
  As a platform operator
  I want to see an aggregated health score for each employer
  So that I can identify employers needing attention

  Background:
    Given the standard test identifiers are configured

  @health-score @happy-path
  Scenario: New employer gets perfect health score
    When I request the health score for the standard employer
    Then the response status code should be 200
    And the health score overall should be at least 80
    And the health score risk level should be "LOW"

  @health-score @with-endorsements
  Scenario: Employer with confirmed endorsements has high success rate
    Given a CONFIRMED endorsement exists for the standard employer
    When I request the health score for the standard employer
    Then the response status code should be 200
    And the endorsement success rate should be 100.0

  @health-score @balance
  Scenario: Employer with positive EA balance has healthy balance score
    Given an EA account exists for the standard employer with balance 50000.00
    When I request the health score for the standard employer
    Then the response status code should be 200
    And the balance health score should be 100.0

  @health-score @components
  Scenario: Health score includes all component scores
    When I request the health score for the standard employer
    Then the response status code should be 200
    And the health score response should contain all components
