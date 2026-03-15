import { test, expect } from '@playwright/test';

test.describe('Notification Center @allure.label.epic:Endorsement_E2E', () => {
  test('notification bell is visible in top bar', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('button', { name: 'Notifications' })).toBeVisible({ timeout: 10_000 });
  });

  test('notification popover opens and shows empty state', async ({ page }) => {
    await page.goto('/');
    const bellButton = page.getByRole('button', { name: 'Notifications' });
    await expect(bellButton).toBeVisible({ timeout: 10_000 });
    await bellButton.click();
    const popover = page.locator('[data-slot="popover-content"]');
    await expect(popover.getByRole('heading', { name: 'Notifications' })).toBeVisible({ timeout: 5_000 });
    await expect(popover.getByText('No notifications yet')).toBeVisible();
  });

  test('connection status indicator is visible', async ({ page }) => {
    await page.goto('/');
    // Connection indicator shows either "Live" (connected) or WifiOff icon (disconnected)
    const header = page.locator('header');
    const liveIndicator = header.getByText('Live');
    const disconnectedTitle = header.locator('[title="WebSocket disconnected"]');
    await expect(liveIndicator.or(disconnectedTitle)).toBeVisible({ timeout: 10_000 });
  });
});
