import { test, expect } from '@playwright/test';
import { createEndorsementViaApi, generateEmployeeId, TEST_IDS } from '../fixtures/test-helpers';

test.describe('Endorsements List Page @allure.label.epic:Endorsement_E2E', () => {
  test('displays endorsement table with column headers', async ({ page }) => {
    await createEndorsementViaApi({ employeeId: generateEmployeeId() });
    await page.goto('/endorsements');
    const main = page.locator('main');
    await expect(main.getByRole('columnheader', { name: /Employee/ })).toBeVisible({ timeout: 10_000 });
    await expect(main.getByRole('columnheader', { name: /Type/ })).toBeVisible();
    await expect(main.getByRole('columnheader', { name: /Status/ })).toBeVisible();
    await expect(main.getByRole('columnheader', { name: /Premium/ })).toBeVisible();
    await expect(main.getByRole('columnheader', { name: /Coverage Start/ })).toBeVisible();
    await expect(main.getByRole('columnheader', { name: /Insurer Ref/ })).toBeVisible();
    await expect(main.getByRole('columnheader', { name: /Created/ })).toBeVisible();
  });

  test('Create Endorsement button navigates to create page', async ({ page }) => {
    await page.goto('/endorsements');
    const createButton = page.getByRole('link', { name: /Create Endorsement/ });
    await createButton.click();
    await expect(page).toHaveURL(/\/endorsements\/new/);
  });

  test('status filter popover opens and shows status options', async ({ page }) => {
    await page.goto('/endorsements');
    const main = page.locator('main');
    const statusButton = main.getByRole('button', { name: /Status/ }).first();
    await expect(statusButton).toBeVisible({ timeout: 5_000 });
    await statusButton.click();
    const popover = page.locator('[data-slot="popover-content"]');
    await expect(popover.getByText('Created')).toBeVisible();
    await expect(popover.getByText('Confirmed')).toBeVisible();
    await expect(popover.getByText('Rejected')).toBeVisible();
  });

  test('shows endorsement data in the table', async ({ page }) => {
    await createEndorsementViaApi({ employeeId: generateEmployeeId() });
    await page.goto('/endorsements');
    const main = page.locator('main');
    await expect(main.locator('table tbody tr').first()).toBeVisible({ timeout: 10_000 });
    await expect(main.locator('table tbody tr [data-slot="badge"]').first()).toBeVisible();
  });

  test('shows empty state when no endorsements found for unknown employer', async ({ page }) => {
    await page.goto('/endorsements');
    const main = page.locator('main');
    const input = main.getByPlaceholder('Employer ID (UUID)');
    await input.clear();
    await input.fill('99999999-9999-9999-9999-999999999999');
    await expect(main.getByText('No endorsements found')).toBeVisible({ timeout: 10_000 });
  });

  test('table sorting toggles on column header click', async ({ page }) => {
    await createEndorsementViaApi({ employeeId: generateEmployeeId() });
    await page.goto('/endorsements');
    const main = page.locator('main');
    await expect(main.locator('table tbody tr').first()).toBeVisible({ timeout: 10_000 });

    // Click Employee header to sort ascending
    const employeeHeader = main.getByRole('columnheader', { name: /Employee/ });
    await employeeHeader.click();
    // Should show ascending sort indicator (ArrowUp svg)
    await expect(employeeHeader.locator('svg')).toBeVisible();
  });

  test('CSV export button is visible and functional', async ({ page }) => {
    await createEndorsementViaApi({ employeeId: generateEmployeeId() });
    await page.goto('/endorsements');
    const main = page.locator('main');
    await expect(main.locator('table tbody tr').first()).toBeVisible({ timeout: 10_000 });
    const exportButton = page.getByRole('button', { name: /Export CSV/ });
    await expect(exportButton).toBeVisible();
    await expect(exportButton).toBeEnabled();
  });

  test('CSV export button disabled when no data', async ({ page }) => {
    await page.goto('/endorsements');
    const main = page.locator('main');
    const input = main.getByPlaceholder('Employer ID (UUID)');
    await input.clear();
    await input.fill('99999999-9999-9999-9999-999999999999');
    await expect(main.getByText('No endorsements found')).toBeVisible({ timeout: 10_000 });
    // Export button should still be visible but disabled when no data
    const exportButton = page.getByRole('button', { name: /Export CSV/ });
    await expect(exportButton).toBeDisabled();
  });

  test('pagination controls are visible with data', async ({ page }) => {
    // Create multiple endorsements
    for (let i = 0; i < 3; i++) {
      await createEndorsementViaApi({ employeeId: generateEmployeeId() });
    }
    await page.goto('/endorsements');
    const main = page.locator('main');
    await expect(main.locator('table tbody tr').first()).toBeVisible({ timeout: 10_000 });
    // Pagination shows "Showing X-Y of Z" text
    await expect(page.getByText(/Showing \d+/)).toBeVisible();
  });

  test('URL persists filter state', async ({ page }) => {
    await createEndorsementViaApi({ employeeId: generateEmployeeId() });
    await page.goto('/endorsements?employerId=11111111-1111-1111-1111-111111111111&statuses=CONFIRMED');
    // URL should contain the filter params
    await expect(page).toHaveURL(/statuses=CONFIRMED/);
    // Status filter should show 1 selected
    const main = page.locator('main');
    const statusButton = main.getByRole('button', { name: /Status/ }).first();
    await expect(statusButton).toBeVisible({ timeout: 5_000 });
    // Badge showing count of selected statuses (inside the status button)
    await expect(statusButton.locator('span[data-slot="badge"]')).toBeVisible();
  });

  test('last updated timestamp and refresh button are visible', async ({ page }) => {
    await createEndorsementViaApi({ employeeId: generateEmployeeId() });
    await page.goto('/endorsements');
    const main = page.locator('main');
    await expect(main.locator('table tbody tr').first()).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(/Updated/)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Refresh endorsements' })).toBeVisible();
  });
});
