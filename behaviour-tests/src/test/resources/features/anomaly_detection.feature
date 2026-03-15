@allure.label.epic:Endorsement_BDD
Feature: Anomaly Detection
  As a platform operator
  I want unusual endorsement patterns to be automatically detected
  So that potential fraud or data quality issues are flagged for review

  Background:
    Given the standard test identifiers are configured

  @anomaly @volume-spike
  Scenario: Volume spike anomaly flagged when many endorsements created for same employer
    Given 25 ADD endorsements exist for the standard employer within 1 hour
    When I request anomaly analysis for the standard employer
    Then the response status code should be 200
    And the anomaly list should contain at least 1 entry
    And the anomaly list should contain an anomaly of type "VOLUME_SPIKE"
    And the anomaly severity should be "HIGH"
    And the anomaly status should be "FLAGGED"

  @anomaly @cycling
  Scenario: Add/delete cycling detected for same employee
    Given an ADD endorsement exists for employee "cycle-employee-001"
    And a DELETE endorsement exists for employee "cycle-employee-001"
    And another ADD endorsement exists for employee "cycle-employee-001"
    When I request anomaly analysis for the standard employer
    Then the response status code should be 200
    And the anomaly list should contain an anomaly of type "ADD_DELETE_CYCLING"
    And the anomaly details should reference employee "cycle-employee-001"

  @anomaly @below-threshold
  Scenario: Below-threshold anomaly not flagged
    Given 2 ADD endorsements exist for the standard employer within 1 hour
    When I request anomaly analysis for the standard employer
    Then the response status code should be 200
    And the anomaly list should be empty

  @anomaly @review
  Scenario: Anomaly review status can be updated
    Given 25 ADD endorsements exist for the standard employer within 1 hour
    And I request anomaly analysis for the standard employer
    And the anomaly list should contain at least 1 entry
    When I update the first anomaly status to "UNDER_REVIEW" with notes "Legitimate bulk onboarding"
    Then the response status code should be 200
    And the anomaly status should be "UNDER_REVIEW"
    And the anomaly review notes should be "Legitimate bulk onboarding"

  @anomaly @suspicious-timing
  Scenario: Suspicious timing anomaly detected for employee added near claim window
    Given an employer "EMPLOYER-ST" with insurer "INSURER-ST"
    And the employer has an EA account with balance 500000.00
    And an endorsement exists for the employer with type "ADD" created 3 days ago
    When the system analyzes the endorsement for anomalies
    Then an anomaly should be flagged with type "SUSPICIOUS_TIMING"
    And the anomaly score should be at least 0.7

  @anomaly @multi-anomaly
  Scenario: Multiple anomaly types detected simultaneously
    Given an employer "EMPLOYER-MA" with insurer "INSURER-MA"
    And the employer has an EA account with balance 500000.00
    And 50 endorsements exist for the employer in the last 24 hours
    And an employee was added and then deleted within 7 days
    When the system analyzes all endorsements for anomalies
    Then at least 1 anomalies should be flagged

  @anomaly @unusual-premium
  Scenario: Unusual premium anomaly flagged for outlier premium amount
    Given an employer "EMPLOYER-UP" with insurer "INSURER-UP"
    And the employer has an EA account with balance 1000000.00
    And 10 endorsements exist for the employer with normal premium 1200.00
    And an endorsement exists for the employer with premium 50000.00
    When the system analyzes all endorsements for anomalies
    Then at least 1 anomalies should be flagged

  @anomaly @dormancy-break
  Scenario: Anomaly detection identifies dormancy break pattern
    Given an employer "EMPLOYER-DB" with insurer "INSURER-DB"
    And the employer has an EA account with balance 500000.00
    And a dormant employee endorsement exists from 120 days ago
    When the system analyzes all endorsements for anomalies
    Then at least 1 anomalies should be flagged

  @anomaly @false-positive
  Scenario: False positive anomaly dismissed by reviewer
    Given an employer "EMPLOYER-FP" with insurer "INSURER-FP"
    And an anomaly exists with status "FLAGGED" and type "VOLUME_SPIKE"
    When a reviewer dismisses the anomaly with notes "Planned onboarding batch"
    Then the anomaly status should be "DISMISSED"
    And the reviewer notes should be "Planned onboarding batch"
