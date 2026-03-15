import { test, expect } from '@playwright/test';

test.describe('EmptyState @allure.label.epic:Endorsement_E2E', () => {
  test('renders default empty state with title and description', async ({ page }) => {
    await page.goto('/iframe.html?id=shared-emptystate--default&viewMode=story');
    await expect(page.getByText('No endorsements found')).toBeVisible();
    await expect(page.getByText('Create your first endorsement to get started.')).toBeVisible();
  });

  test('renders with action button linking to href', async ({ page }) => {
    await page.goto('/iframe.html?id=shared-emptystate--with-action&viewMode=story');
    await expect(page.getByText('No endorsements found')).toBeVisible();
    const actionButton = page.getByRole('link', { name: 'Create Endorsement' });
    await expect(actionButton).toBeVisible();
  });

  test('renders with callback action button', async ({ page }) => {
    await page.goto('/iframe.html?id=shared-emptystate--with-callback&viewMode=story');
    await expect(page.getByText('No results')).toBeVisible();
    const resetButton = page.getByRole('button', { name: 'Reset Filters' });
    await expect(resetButton).toBeVisible();
  });
});
