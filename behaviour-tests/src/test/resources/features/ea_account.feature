@allure.label.epic:Endorsement_BDD
Feature: EA Account Balance Management
  As an employer
  I want to view my EA account balance
  So that I can track available funds for endorsements

  Background:
    Given the standard test identifiers are configured

  @ea-account @happy-path
  Scenario: Get EA account balance
    Given an EA account exists with a balance of 100000.00
    When I get the EA account for the standard employer and insurer
    Then the response status code should be 200
    And the EA account balance should be 100000.00
    And the EA account reserved amount should be 0.00
    And the EA account available balance should be 100000.00

  @ea-account @error
  Scenario: Return 404 for non-existent EA account
    When I get an EA account with random employer and insurer IDs
    Then the response status code should be 404

  @ea-account @reservation
  Scenario: Reflect reservation after ADD endorsement creation
    Given an EA account exists with a balance of 50000.00
    And I create an "ADD" endorsement with premium 1500.00
    When I get the EA account for the standard employer and insurer
    Then the response status code should be 200
    And the EA account reserved amount should be 1500.00
    And the EA account available balance should be 48500.00

  @ea-account @no-reservation
  Scenario: No balance change for DELETE endorsement
    Given an EA account exists with a balance of 50000.00
    And I create a "DELETE" endorsement without premium
    When I get the EA account for the standard employer and insurer
    Then the response status code should be 200
    And the EA account reserved amount should be 0.00
    And the EA account available balance should be 50000.00
