@allure.label.epic:Endorsement_BDD
Feature: Cross-Insurer Benchmarking
  As a platform operator
  I want to compare performance metrics across insurers
  So that I can identify best and worst performers

  @benchmarking @list
  Scenario: Get benchmarks for all active insurers
    When I request cross-insurer benchmarks
    Then the response status code should be 200
    And the benchmarks list should contain at least 4 entries
    And each benchmark should have insurer name and code
    And each benchmark should have processing time metrics

  @benchmarking @sorting
  Scenario: Benchmarks are sorted by STP rate
    When I request cross-insurer benchmarks
    Then the response status code should be 200
    And the benchmarks should be sorted by STP rate descending
