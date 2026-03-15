import { test, expect } from '@playwright/test';

test.describe('Sidebar @allure.label.epic:Endorsement_E2E', () => {
  test('renders sidebar with all nav items on dashboard route', async ({ page }) => {
    await page.goto('/iframe.html?id=layout-sidebar--dashboard-active&viewMode=story');
    await expect(page.locator('span', { hasText: 'Plum' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Dashboard' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Endorsements' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Batches' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'EA Accounts' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Insurers' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Reconciliation' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Intelligence' })).toBeVisible();
  });

  test('highlights Endorsements nav item', async ({ page }) => {
    await page.goto('/iframe.html?id=layout-sidebar--endorsements-active&viewMode=story');
    await expect(page.getByRole('link', { name: 'Endorsements' })).toBeVisible();
  });

  test('highlights EA Accounts nav item', async ({ page }) => {
    await page.goto('/iframe.html?id=layout-sidebar--ea-accounts-active&viewMode=story');
    await expect(page.getByRole('link', { name: 'EA Accounts' })).toBeVisible();
  });
});
