import { test, expect } from '@playwright/test';
import { createEndorsementViaApi, generateEmployeeId, TEST_IDS } from '../fixtures/test-helpers';

test.describe('Dashboard Page @allure.label.epic:Endorsement_E2E', () => {
  test('displays summary cards', async ({ page }) => {
    await page.goto('/');
    const main = page.locator('main');
    await expect(main.locator('p', { hasText: 'Total' })).toBeVisible({ timeout: 10_000 });
    await expect(main.locator('p', { hasText: 'Pending' })).toBeVisible();
    await expect(main.locator('p', { hasText: 'Confirmed' })).toBeVisible();
    await expect(main.locator('p', { hasText: 'Failed' })).toBeVisible();
  });

  test('shows EA account balance card', async ({ page }) => {
    await page.goto('/');
    const main = page.locator('main');
    await expect(main.getByText('EA Account', { exact: true })).toBeVisible({ timeout: 10_000 });
    const balanceLabel = main.locator('p', { hasText: 'Balance' });
    const noAccountMsg = main.getByText('No EA account found');
    await expect(balanceLabel.or(noAccountMsg)).toBeVisible({ timeout: 10_000 });
  });

  test('shows recent endorsements table after creating one', async ({ page }) => {
    await createEndorsementViaApi({ employeeId: generateEmployeeId() });
    await page.goto('/');
    const main = page.locator('main');
    await expect(main.getByText('Recent Endorsements')).toBeVisible({ timeout: 10_000 });
    await expect(main.locator('table tbody tr').first()).toBeVisible({ timeout: 10_000 });
  });

  test('"View all" link navigates to endorsements list', async ({ page }) => {
    await page.goto('/');
    const viewAllLink = page.getByRole('link', { name: /View all/ });
    await viewAllLink.click();
    await expect(page).toHaveURL(/\/endorsements/);
  });

  test('employer ID input has correct default value', async ({ page }) => {
    await page.goto('/');
    const main = page.locator('main');
    const employerInput = main.locator('input').first();
    await expect(employerInput).toHaveValue(TEST_IDS.employerId);
  });

  test('KPI card "Total" navigates to endorsements list', async ({ page }) => {
    await page.goto('/');
    const main = page.locator('main');
    await expect(main.locator('p', { hasText: 'Total' })).toBeVisible({ timeout: 10_000 });
    // Click the Total card (it's a Link)
    const totalCard = main.locator('p', { hasText: 'Total' }).locator('..').locator('..');
    await totalCard.click();
    await expect(page).toHaveURL(/\/endorsements\?employerId=/);
  });

  test('shows Active Batches and Outstanding Items cards', async ({ page }) => {
    await page.goto('/');
    const main = page.locator('main');
    await expect(main.locator('p', { hasText: 'Total' })).toBeVisible({ timeout: 10_000 });
    await expect(main.getByText('Active Batches')).toBeVisible();
    await expect(main.getByText('Outstanding Items')).toBeVisible();
  });

  test('Active Batches card navigates to batches page', async ({ page }) => {
    await page.goto('/');
    const main = page.locator('main');
    await expect(main.getByText('Active Batches')).toBeVisible({ timeout: 10_000 });
    await main.getByText('Active Batches').click();
    await expect(page).toHaveURL(/\/endorsements\/batches/);
  });

  test('shows last updated timestamp with refresh button', async ({ page }) => {
    await page.goto('/');
    const main = page.locator('main');
    await expect(main.locator('p', { hasText: 'Total' })).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(/Dashboard updated/)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Refresh dashboard' })).toBeVisible();
  });
});
