import { test, expect } from '@playwright/test';

test.describe('Intelligence - Cross-Tab Navigation @allure.label.epic:Intelligence_E2E', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/intelligence');
  });

  test('should navigate between all intelligence tabs', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    // Click Anomalies tab and verify content loads
    const anomaliesTab = main.getByRole('tab', { name: /Anomalies/ });
    await anomaliesTab.click();
    await expect(anomaliesTab).toHaveAttribute('data-active', '');
    // Anomalies tab renders a table with "Employer" column or empty state
    const anomalyContent = main.getByText('Flagged Anomalies');
    await expect(anomalyContent).toBeVisible({ timeout: 10_000 });

    // Click Forecasts tab and verify content loads
    const forecastsTab = main.getByRole('tab', { name: /Forecasts/ });
    await forecastsTab.click();
    await expect(forecastsTab).toHaveAttribute('data-active', '');
    await expect(anomaliesTab).not.toHaveAttribute('data-active', '');
    // Forecasts tab shows Employer ID input and Generate Forecast button
    const forecastContent = main.getByText(/Employer ID|Generate Forecast|No forecast data/);
    await expect(forecastContent.first()).toBeVisible({ timeout: 10_000 });

    // Click Error Resolution tab and verify content loads
    const errorResolutionTab = main.getByRole('tab', { name: /Error Resolution/ });
    await errorResolutionTab.click();
    await expect(errorResolutionTab).toHaveAttribute('data-active', '');
    await expect(forecastsTab).not.toHaveAttribute('data-active', '');
    // Error Resolution tab renders stats cards or empty state
    const errorContent = main.getByText(/Total Resolutions|No error resolution data/);
    await expect(errorContent.first()).toBeVisible({ timeout: 10_000 });

    // Click Process Mining tab and verify content loads
    const processMiningTab = main.getByRole('tab', { name: /Process Mining/ });
    await processMiningTab.click();
    await expect(processMiningTab).toHaveAttribute('data-active', '');
    await expect(errorResolutionTab).not.toHaveAttribute('data-active', '');
    const miningContent = main.getByText(/Overall STP Rate|No process mining data/);
    await expect(miningContent.first()).toBeVisible({ timeout: 10_000 });
  });

  test('should preserve default tab when navigating away and back', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    // Verify default tab (Anomalies) is active on initial load
    const anomaliesTab = main.getByRole('tab', { name: /Anomalies/ });
    await expect(anomaliesTab).toHaveAttribute('data-active', '');

    // Navigate away to Dashboard
    await page.goto('/');
    await expect(page.locator('main')).toBeVisible({ timeout: 10_000 });

    // Navigate back to Intelligence
    await page.goto('/intelligence');
    const mainAfterReturn = page.locator('main');
    await expect(mainAfterReturn.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    // Default tab (Anomalies) should be active again since state is component-level
    const anomaliesTabAfterReturn = mainAfterReturn.getByRole('tab', { name: /Anomalies/ });
    await expect(anomaliesTabAfterReturn).toHaveAttribute('data-active', '');
  });

  test('should load intelligence page from sidebar navigation', async ({ page }) => {
    // Start from a different page
    await page.goto('/');
    await expect(page.locator('main')).toBeVisible({ timeout: 10_000 });

    // Click the Intelligence link in the sidebar
    const sidebar = page.locator('aside');
    const intelligenceLink = sidebar.getByRole('link', { name: /Intelligence/ });
    await expect(intelligenceLink).toBeVisible({ timeout: 5_000 });
    await intelligenceLink.click();

    // Verify we navigated to /intelligence
    await expect(page).toHaveURL(/\/intelligence/);
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
  });

  test('should show all four tab triggers in the tabs list', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    // Verify all four tabs are rendered in the tab list
    await expect(main.getByRole('tab', { name: /Anomalies/ })).toBeVisible({ timeout: 5_000 });
    await expect(main.getByRole('tab', { name: /Forecasts/ })).toBeVisible({ timeout: 5_000 });
    await expect(main.getByRole('tab', { name: /Error Resolution/ })).toBeVisible({ timeout: 5_000 });
    await expect(main.getByRole('tab', { name: /Process Mining/ })).toBeVisible({ timeout: 5_000 });
  });

  test('should display page header with description', async ({ page }) => {
    const main = page.locator('main');

    // Verify the page header title
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    // Verify the page header description
    await expect(
      main.getByText('AI-powered anomaly detection, forecasting, error resolution, and process mining'),
    ).toBeVisible({ timeout: 5_000 });
  });
});
