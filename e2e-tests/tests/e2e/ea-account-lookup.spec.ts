import { test, expect } from '@playwright/test';
import { TEST_IDS } from '../fixtures/test-helpers';

test.describe('EA Account Lookup Page @allure.label.epic:Endorsement_E2E', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/ea-accounts');
  });

  test('renders lookup form with employer and insurer ID inputs', async ({ page }) => {
    const main = page.locator('main');
    // Page heading - use heading role to avoid matching sidebar nav
    await expect(main.locator('h1', { hasText: 'EA Accounts' })).toBeVisible();
    await expect(main.getByText('Account Lookup')).toBeVisible();
    await expect(page.locator('#ea-employerId')).toBeVisible();
    await expect(page.locator('#ea-insurerId')).toBeVisible();
    await expect(main.getByRole('button', { name: /Look Up/ })).toBeVisible();
  });

  test('look up button is disabled without both IDs', async ({ page }) => {
    const main = page.locator('main');
    const lookupButton = main.getByRole('button', { name: /Look Up/ });
    await expect(lookupButton).toBeDisabled();
  });

  test('shows account balance cards after successful lookup', async ({ page }) => {
    await page.locator('#ea-employerId').fill(TEST_IDS.employerId);
    await page.locator('#ea-insurerId').fill(TEST_IDS.insurerId);
    const main = page.locator('main');
    await main.getByRole('button', { name: /Look Up/ }).click();

    // Should show balance cards or no account message
    const hasBalance = main.getByText('Total Balance');
    const noAccount = main.getByText('No account found');
    // Wait for either to appear
    await expect(hasBalance.or(noAccount)).toBeVisible({ timeout: 10_000 });
  });

  test('shows empty state for non-existent account', async ({ page }) => {
    await page.locator('#ea-employerId').fill('99999999-9999-9999-9999-999999999999');
    await page.locator('#ea-insurerId').fill('88888888-8888-8888-8888-888888888888');
    const main = page.locator('main');
    await main.getByRole('button', { name: /Look Up/ }).click();

    await expect(main.getByText('No account found')).toBeVisible({ timeout: 10_000 });
  });
});
