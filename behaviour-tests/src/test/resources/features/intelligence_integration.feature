@allure.label.epic:Endorsement_BDD
@intelligence
Feature: Intelligence Cross-Feature Integration
  Intelligence features work together to provide comprehensive insights

  Background:
    Given the standard test identifiers are configured

  @intelligence @anomaly-error-resolution
  Scenario: Anomaly detection and error resolution work together on a flagged endorsement
    Given an employer "EMPLOYER-INT" with insurer "INSURER-INT"
    And the employer has an EA account with balance 500000.00
    And an endorsement exists that triggers a volume spike anomaly
    And the endorsement is later rejected with error "DOB format invalid"
    When the system processes the rejection
    Then an anomaly should exist for the employer
    And an error resolution should exist for the endorsement
    And the error resolution should have confidence above 0.9

  @intelligence @forecast-activity
  Scenario: Forecast incorporates recent endorsement activity including anomalies
    Given an employer "EMPLOYER-FA" with insurer "INSURER-FA"
    And the employer has an EA account with balance 200000.00
    And the employer has historical endorsement data for 90 days
    When a balance forecast is generated after recent activity
    Then the forecast should reflect the current burn rate
    And the forecast narrative should mention the balance projection
