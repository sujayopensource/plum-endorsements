import { test, expect } from '@playwright/test';

test.describe('Accessibility @allure.label.epic:Endorsement_E2E', () => {
  test('skip to content link is present', async ({ page }) => {
    await page.goto('/');
    const skipLink = page.getByRole('link', { name: 'Skip to content' });
    // Skip link exists but is sr-only by default
    await expect(skipLink).toBeAttached();
  });

  test('main content has correct id for skip navigation', async ({ page }) => {
    await page.goto('/');
    const mainContent = page.locator('#main-content');
    await expect(mainContent).toBeAttached();
  });

  test('breadcrumb navigation has aria-label', async ({ page }) => {
    await page.goto('/endorsements');
    const breadcrumbNav = page.locator('nav[aria-label="Breadcrumb"]');
    await expect(breadcrumbNav).toBeVisible({ timeout: 10_000 });
  });

  test('mobile menu button has aria-label', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/');
    const menuButton = page.getByRole('button', { name: 'Open navigation menu' });
    await expect(menuButton).toBeVisible({ timeout: 10_000 });
  });

  test('ARIA live region exists for screen reader announcements', async ({ page }) => {
    await page.goto('/');
    const liveRegion = page.locator('#live-region');
    await expect(liveRegion).toBeAttached();
    await expect(liveRegion).toHaveAttribute('aria-live', 'polite');
  });
});
