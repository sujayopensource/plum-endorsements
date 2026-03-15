@allure.label.epic:Endorsement_BDD
Feature: Self-Service Insurer Onboarding
  As a platform administrator
  I want to onboard new insurers via API
  So that new insurance partners can be added without code changes

  @onboarding @create
  Scenario: Create a new insurer configuration
    When I create an insurer with name "BDD Test Insurer" and code "BDD_TEST"
    Then the response status code should be 201
    And the insurer name should be "BDD Test Insurer"
    And the insurer code should be "BDD_TEST"
    And the insurer should be active

  @onboarding @update
  Scenario: Update an existing insurer configuration
    Given a new insurer "Update Test" with code "UPD_TST" exists
    When I update the insurer name to "Updated Name"
    Then the response status code should be 200
    And the insurer name should be "Updated Name"

  @onboarding @deactivate
  Scenario: Deactivate an insurer
    Given a new insurer "Deactivate Test" with code "DEACT_T" exists
    When I deactivate the insurer
    Then the response status code should be 200
    And the insurer should not be active
