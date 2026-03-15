@allure.label.epic:Endorsement_BDD
Feature: Audit Logging
  As a compliance officer
  I want all operations to be automatically logged
  So that we have a full audit trail for regulatory compliance

  Background:
    Given the standard test identifiers are configured

  @audit @listing
  Scenario: Audit logs are accessible via API
    When I request the audit logs
    Then the response status code should be 200
    And the audit log response should be paginated

  @audit @capture
  Scenario: Creating an endorsement generates an audit log
    Given an EA account exists for the standard employer with balance 100000.00
    When I create an ADD endorsement with premium 1000.00
    And I request the audit logs
    Then the response status code should be 200
    And the audit logs should contain at least 1 entry
