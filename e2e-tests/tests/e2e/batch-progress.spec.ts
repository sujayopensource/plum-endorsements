import { test, expect } from '@playwright/test';
import { TEST_IDS } from '../fixtures/test-helpers';

test.describe('Batch Progress Page @allure.label.epic:Endorsement_E2E', () => {
  test('renders page header and employer ID input', async ({ page }) => {
    await page.goto('/endorsements/batches');
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Batch Progress' })).toBeVisible({ timeout: 10_000 });
    await expect(main.getByText('Track endorsement batch submissions')).toBeVisible();
    // Employer ID input with default value
    const input = main.locator('input').first();
    await expect(input).toHaveValue(TEST_IDS.employerId);
  });

  test('shows empty state or batch data', async ({ page }) => {
    await page.goto('/endorsements/batches');
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Batch Progress' })).toBeVisible({ timeout: 10_000 });
    // Either shows batch table or empty state
    const batchTable = main.locator('table');
    const emptyState = main.getByText('No batches found');
    await expect(batchTable.or(emptyState)).toBeVisible({ timeout: 10_000 });
  });

  test('batch table has correct column headers when data exists', async ({ page }) => {
    await page.goto('/endorsements/batches');
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Batch Progress' })).toBeVisible({ timeout: 10_000 });
    const batchTable = main.locator('table');
    const emptyState = main.getByText('No batches found');
    await expect(batchTable.or(emptyState)).toBeVisible({ timeout: 10_000 });
    if (await batchTable.isVisible()) {
      await expect(main.getByRole('columnheader', { name: /Batch ID/ })).toBeVisible();
      await expect(main.getByRole('columnheader', { name: /Insurer/ })).toBeVisible();
      await expect(main.getByRole('columnheader', { name: /Status/ })).toBeVisible();
    }
  });

  test('navigable from sidebar', async ({ page }) => {
    await page.goto('/');
    const sidebar = page.locator('aside');
    const batchesLink = sidebar.getByRole('link', { name: /Batches/ });
    await expect(batchesLink).toBeVisible({ timeout: 5_000 });
    await batchesLink.click();
    await expect(page).toHaveURL(/\/endorsements\/batches/);
  });
});
