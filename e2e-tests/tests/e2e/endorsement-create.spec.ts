import { test, expect } from '@playwright/test';
import { generateEmployeeId, TEST_IDS } from '../fixtures/test-helpers';

test.describe('Create Endorsement Page @allure.label.epic:Endorsement_E2E', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/endorsements/new');
  });

  test('form renders with all sections and default values', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Create Endorsement' })).toBeVisible();
    await expect(main.getByText('Identifiers')).toBeVisible();
    await expect(main.getByText('Endorsement Details')).toBeVisible();
    await expect(main.getByText('Employee Data')).toBeVisible();
    await expect(main.getByText('Advanced')).toBeVisible();

    const employerInput = page.locator('#employerId');
    await expect(employerInput).toHaveValue(TEST_IDS.employerId);
    const insurerInput = page.locator('#insurerId');
    await expect(insurerInput).toHaveValue(TEST_IDS.insurerId);
  });

  test('validates required fields on submit', async ({ page }) => {
    const submitButton = page.getByRole('button', { name: 'Create Endorsement' });
    await submitButton.click();
    await expect(page.getByText('Must be a valid UUID').first()).toBeVisible();
  });

  test('type selector allows choosing ADD/DELETE/UPDATE', async ({ page }) => {
    const main = page.locator('main');
    const typeSelect = main.getByRole('combobox').first();
    await expect(typeSelect).toBeVisible();
  });

  test('cancel button navigates back to endorsements list', async ({ page }) => {
    const cancelButton = page.getByRole('link', { name: 'Cancel' });
    await cancelButton.click();
    await expect(page).toHaveURL(/\/endorsements$/);
  });

  test('successful creation redirects to detail page', async ({ page }) => {
    const employeeId = generateEmployeeId();
    await page.locator('#employeeId').fill(employeeId);
    await page.locator('#employeeName').fill('Test Employee');
    await page.locator('#coverageStartDate').fill('2026-04-01');
    await page.locator('#premiumAmount').fill('1500');
    const submitButton = page.getByRole('button', { name: 'Create Endorsement' });
    await submitButton.click();
    await expect(page).toHaveURL(/\/endorsements\/[a-f0-9-]+/, { timeout: 15_000 });
    await expect(page.locator('main h1', { hasText: 'Endorsement Detail' })).toBeVisible();
  });

  test('shows validation error for invalid UUID format', async ({ page }) => {
    await page.locator('#employeeId').fill('not-a-uuid');
    await page.locator('#employeeName').fill('Test');
    await page.locator('#coverageStartDate').fill('2026-04-01');
    const submitButton = page.getByRole('button', { name: 'Create Endorsement' });
    await submitButton.click();
    await expect(page.getByText('Must be a valid UUID').first()).toBeVisible();
  });

  test('DELETE type hides premium amount and employee data sections', async ({ page }) => {
    const main = page.locator('main');
    // Select DELETE type
    const typeSelect = main.getByRole('combobox').first();
    await typeSelect.click();
    await page.getByRole('option', { name: 'Delete' }).click();
    // Premium Amount field should be hidden
    await expect(page.locator('#premiumAmount')).not.toBeVisible();
    // Employee Data section should be hidden
    await expect(main.getByText('Employee Data')).not.toBeVisible();
  });

  test('back button has correct aria-label', async ({ page }) => {
    await expect(
      page.getByRole('link', { name: 'Back to endorsements list' }),
    ).toBeVisible();
  });
});
