@allure.label.epic:Endorsement_BDD
Feature: Process Mining
  As a platform operator
  I want to analyze endorsement lifecycle patterns across insurers
  So that I can identify bottlenecks and optimize processing efficiency

  Background:
    Given the standard test identifiers are configured

  @process-mining @bottleneck
  Scenario: Bottleneck identified for slow insurer transitions
    Given endorsements exist with the following lifecycle times for the mock insurer:
      | fromStatus             | toStatus       | avgDurationMinutes |
      | PROVISIONALLY_COVERED  | QUEUED_FOR_BATCH | 5                |
      | QUEUED_FOR_BATCH       | BATCH_SUBMITTED  | 15               |
      | BATCH_SUBMITTED        | CONFIRMED        | 1440             |
    When I request process mining analysis for the mock insurer
    Then the response status code should be 200
    And the bottleneck analysis should identify "BATCH_SUBMITTED" to "CONFIRMED" as the slowest transition
    And the bottleneck average duration should be greater than 1000 minutes

  @process-mining @stp-rate
  Scenario: STP rate calculated across endorsements
    Given 80 endorsements completed with status "CONFIRMED" for the mock insurer
    And 15 endorsements completed with status "REJECTED" for the mock insurer
    And 5 endorsements completed with status "FAILED" for the mock insurer
    When I request the STP rate for the mock insurer
    Then the response status code should be 200
    And the STP rate should be 80.0
    And the total endorsements processed should be 100
    And the successful count should be 80

  @process-mining @happy-path
  Scenario: Happy path percentage computed
    Given 70 endorsements followed the happy path for the mock insurer
    And 30 endorsements deviated from the happy path for the mock insurer
    When I request process mining analysis for the mock insurer
    Then the response status code should be 200
    And the STP rate should be 70.0

  @process-mining @stp-trend
  Scenario: STP rate trend shows historical data
    Given STP rate snapshots exist for the mock insurer over the last 3 days
    When I request the STP rate trend for the mock insurer
    Then the response status code should be 200
    And the trend should contain at least 3 data points
    And the trend current rate should be greater than 0

  @process-mining @deviated-paths
  Scenario: Process mining detects deviated lifecycle paths
    Given an insurer "INSURER-DEV" has processed endorsements
    And 3 endorsements followed the happy path
    And 2 endorsements had rejected-and-retried paths
    When process mining analysis is triggered for insurer "INSURER-DEV"
    Then the STP rate should be 60%
    And the rejected-to-queued transition should be identified

  @process-mining @insurer-comparison
  Scenario: Process mining identifies fastest and slowest insurers
    Given insurer "FAST-INS" has average lifecycle of 2 hours
    And insurer "SLOW-INS" has average lifecycle of 24 hours
    When process mining insights are generated
    Then the insights should identify "SLOW-INS" as having a bottleneck
    And "FAST-INS" should have no bottleneck insights

  @process-mining @overall-stp
  Scenario: STP rate calculated correctly across all insurers
    Given 3 insurers with different processing patterns
    When the overall STP rate is requested
    Then the response should include per-insurer breakdown
    And the overall rate should be the average of per-insurer rates
