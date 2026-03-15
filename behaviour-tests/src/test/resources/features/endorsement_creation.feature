@allure.label.epic:Endorsement_BDD
Feature: Endorsement Creation
  As an employer
  I want to create insurance endorsements for my employees
  So that I can manage their coverage changes

  Background:
    Given the standard test identifiers are configured

  @creation @happy-path
  Scenario: Create ADD endorsement with valid data
    When I create an "ADD" endorsement with premium 1200.00
    Then the response status code should be 201
    And the endorsement status should be "PROVISIONALLY_COVERED"
    And the endorsement type should be "ADD"
    And the response should contain a non-null "id"
    And the response should contain a non-null "idempotencyKey"
    And the response should contain a non-null "createdAt"

  @creation @idempotency
  Scenario: Auto-generate idempotency key from request fields
    When I create an "ADD" endorsement with premium 500.00
    Then the response status code should be 201
    And the idempotency key should be generated from request fields

  @creation @idempotency
  Scenario: Return existing endorsement for duplicate idempotency key
    Given I create an endorsement with idempotency key "unique-key-123"
    When I create another endorsement with the same idempotency key "unique-key-123"
    Then the response status code should be 201
    And both responses should return the same endorsement ID

  @creation @delete
  Scenario: Create DELETE endorsement without provisional coverage
    When I create a "DELETE" endorsement without premium
    Then the response status code should be 201
    And the endorsement type should be "DELETE"
    And the endorsement status should be "PROVISIONALLY_COVERED"
    And no provisional coverage should exist for this endorsement

  @creation @update
  Scenario: Create UPDATE endorsement without EA reservation
    Given an EA account exists with a balance of 50000.00
    When I create an "UPDATE" endorsement with premium 1000.00
    Then the response status code should be 201
    And the endorsement type should be "UPDATE"
    And the EA account reserved amount should be 0.00
    And the EA account available balance should be 50000.00

  @creation @ea-balance
  Scenario: Reserve EA funds for ADD endorsement with sufficient balance
    Given an EA account exists with a balance of 50000.00
    When I create an "ADD" endorsement with premium 1200.00
    Then the response status code should be 201
    And the EA account balance should be 50000.00
    And the EA account reserved amount should be 1200.00
    And the EA account available balance should be 48800.00

  @creation @ea-balance
  Scenario: Do not reserve funds when EA balance is insufficient
    Given an EA account exists with a balance of 500.00
    When I create an "ADD" endorsement with premium 1200.00
    Then the response status code should be 201
    And the endorsement status should be "PROVISIONALLY_COVERED"
    And the EA account reserved amount should be 0.00
    And the EA account available balance should be 500.00

  @creation @validation
  Scenario: Reject request with missing required fields
    When I create an endorsement with only the type "ADD"
    Then the response status code should be 400
    And the response should contain a "Validation Error" title

  @creation @validation
  Scenario: Reject request with invalid endorsement type
    When I create an endorsement with invalid type "INVALID_TYPE"
    Then the response status code should be 400 or 500
