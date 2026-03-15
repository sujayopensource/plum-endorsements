@allure.label.epic:Endorsement_BDD
Feature: EA Balance Optimization
  As an employer
  I want EA balance reservations to be optimized by endorsement type
  So that only ADD endorsements consume available balance

  Background:
    Given the standard test identifiers are configured

  @optimization @priority
  Scenario: DELETE endorsement does not require EA balance check
    Given an EA account exists with a balance of 50000.00
    When I create a "DELETE" endorsement without premium
    Then the response status code should be 201
    And the EA account reserved amount should be 0.00
    And the EA account available balance should be 50000.00

  @optimization @priority
  Scenario: ADD endorsement reserves from EA balance
    Given an EA account exists with a balance of 100000.00
    When I create an "ADD" endorsement with premium 5000.00
    Then the response status code should be 201
    And the EA account reserved amount should be 5000.00
    And the EA account available balance should be 95000.00

  @optimization @priority
  Scenario: Multiple ADD endorsements accumulate reservations
    Given an EA account exists with a balance of 100000.00
    When I create an "ADD" endorsement with premium 3000.00
    And I create another ADD endorsement with premium 2000.00
    Then the EA account reserved amount should be 5000.00
    And the EA account available balance should be 95000.00

  @optimization @alert
  Scenario: Insufficient balance still creates endorsement
    Given an EA account exists with a balance of 100.00
    When I create an "ADD" endorsement with premium 5000.00
    Then the response status code should be 201
    And the EA account reserved amount should be 0.00
    And the EA account available balance should be 100.00
