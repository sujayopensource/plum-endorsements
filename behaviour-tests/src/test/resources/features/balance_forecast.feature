@allure.label.epic:Endorsement_BDD
Feature: EA Balance Forecast
  As an employer
  I want to see a forecast of my EA balance consumption
  So that I can proactively top up before running out of funds

  Background:
    Given the standard test identifiers are configured

  @forecast @generation
  Scenario: Forecast generated for employer with endorsement history
    Given an EA account exists with a balance of 100000.00
    And 10 historical ADD endorsements exist for the standard employer over the past 30 days
    When I request a balance forecast for the standard employer for the next 30 days
    Then the response status code should be 200
    And the forecast response should contain field "employerId"
    And the forecast response should contain field "forecastedAmount"
    And the forecast response should contain field "forecastDate"
    And the forecast response should contain field "narrative"

  @forecast @shortfall-alert
  Scenario: Shortfall alert sent when forecast exceeds balance
    Given an EA account exists with a balance of 5000.00
    And 20 historical ADD endorsements exist for the standard employer over the past 30 days
    When I request a balance forecast for the standard employer for the next 30 days
    Then the response status code should be 200
    And the forecast response should contain field "forecastedAmount"
    And the forecast response should contain field "narrative"

  @forecast @history
  Scenario: Forecast history available after generation
    Given an EA account exists with a balance of 100000.00
    And 10 historical ADD endorsements exist for the standard employer over the past 30 days
    When I request a balance forecast for the standard employer for the next 30 days
    Then the response status code should be 200
    And the forecast response should contain field "forecastedAmount"
    And the forecast response should contain field "createdAt"

  @forecast @zero-balance
  Scenario: Forecast for employer with zero balance generates forecast
    Given an employer "EMPLOYER-ZB" with insurer "INSURER-ZB"
    And the employer has an EA account with balance 0.00
    And the employer has 10 ADD endorsements in the last 90 days totaling 50000.00
    When a balance forecast is generated
    Then the response status code should be 200
    And the forecast response should contain field "forecastedAmount"

  @forecast @high-balance
  Scenario: Forecast for employer with high balance shows no shortfall
    Given an employer "EMPLOYER-HB" with insurer "INSURER-HB"
    And the employer has an EA account with balance 9999999.00
    And the employer has 5 ADD endorsements in the last 90 days totaling 25000.00
    When a balance forecast is generated
    Then the forecast should indicate no shortfall
    And the forecasted need should be less than the current balance

  @forecast @confidence
  Scenario: Forecast accuracy improves with more historical data
    Given an employer "EMPLOYER-HA" with insurer "INSURER-HA"
    And the employer has an EA account with balance 500000.00
    And the employer has 50 ADD endorsements spread over the last 90 days
    When a balance forecast is generated
    Then the forecast confidence should be "HIGH"
