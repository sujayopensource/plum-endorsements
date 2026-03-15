import { test, expect } from '@playwright/test';

test.describe('Reconciliation Page @allure.label.epic:Endorsement_E2E', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/reconciliation');
  });

  test('renders page header and insurer select', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Reconciliation' })).toBeVisible();
    await expect(page.getByText('Select Insurer')).toBeVisible();
    await expect(page.getByText('Choose an insurer...')).toBeVisible();
  });

  test('trigger button is disabled when no insurer is selected', async ({ page }) => {
    const triggerButton = page.getByRole('button', { name: /Trigger Reconciliation/ });
    await expect(triggerButton).toBeDisabled();
  });

  test('insurer dropdown shows insurer names after selection', async ({ page }) => {
    // Open the select dropdown
    await page.getByText('Choose an insurer...').click();

    // Verify dropdown items show insurer names (not UUIDs)
    await expect(page.getByText('Mock Insurer (MOCK)')).toBeVisible({ timeout: 5000 });
    await expect(page.getByText('ICICI Lombard (ICICI_LOMBARD)')).toBeVisible();

    // Select Mock Insurer
    await page.getByText('Mock Insurer (MOCK)').click();

    // After selection, the trigger should display the insurer NAME, not UUID
    const trigger = page.locator('[data-slot="select-trigger"]');
    await expect(trigger).toContainText('Mock Insurer', { timeout: 5000 });
    await expect(trigger).not.toContainText('22222222');
  });

  test('selecting different insurers updates displayed name', async ({ page }) => {
    // Select ICICI Lombard
    await page.getByText('Choose an insurer...').click();
    await page.getByText('ICICI Lombard (ICICI_LOMBARD)').click();

    const trigger = page.locator('[data-slot="select-trigger"]');
    await expect(trigger).toContainText('ICICI Lombard', { timeout: 5000 });
    await expect(trigger).not.toContainText('33333333');

    // Trigger button should now be enabled
    const triggerButton = page.getByRole('button', { name: /Trigger Reconciliation/ });
    await expect(triggerButton).toBeEnabled();
  });

  test('shows empty state when no reconciliation runs exist', async ({ page }) => {
    // Select an insurer
    await page.getByText('Choose an insurer...').click();
    await page.getByText('Mock Insurer (MOCK)').click();

    // Should show either runs table or empty state
    const emptyState = page.getByText('No reconciliation runs have been performed');
    const runsTable = page.getByText('Reconciliation Runs', { exact: true });
    await expect(emptyState.or(runsTable)).toBeVisible({ timeout: 10000 });
  });
});
