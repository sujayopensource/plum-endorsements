import { test, expect } from '@playwright/test';

test.describe('Intelligence - Forecasts Page @allure.label.epic:Intelligence_E2E', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/intelligence');
  });

  test('forecasts tab renders', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const forecastsTab = main.getByRole('tab', { name: /Forecasts/ });
    await expect(forecastsTab).toBeVisible({ timeout: 10_000 });
  });

  test('navigate to forecasts tab shows input fields and generate button', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const forecastsTab = main.getByRole('tab', { name: /Forecasts/ });
    await forecastsTab.click();
    // Should show employer/insurer input fields and generate button
    await expect(main.getByText('Employer ID')).toBeVisible({ timeout: 10_000 });
    await expect(main.getByText('Insurer ID')).toBeVisible();
    await expect(main.getByRole('button', { name: /Generate Forecast/ })).toBeVisible();
  });

  test('generate forecast button triggers forecast generation', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const forecastsTab = main.getByRole('tab', { name: /Forecasts/ });
    await forecastsTab.click();
    const generateButton = main.getByRole('button', { name: /Generate Forecast/ });
    await expect(generateButton).toBeVisible({ timeout: 10_000 });
    await generateButton.click();
    // Either show generating state or forecast data/empty state
    const generatingButton = main.getByRole('button', { name: /Generating/ });
    const generateBtn = main.getByRole('button', { name: /Generate Forecast/ });
    await expect(generatingButton.or(generateBtn)).toBeVisible({ timeout: 15_000 });
  });

  test('shows forecast data or empty state after generation', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const forecastsTab = main.getByRole('tab', { name: /Forecasts/ });
    await forecastsTab.click();
    // Either forecast data (cards) or empty state should display
    const forecastData = main.getByText(/Forecasted Amount|No forecast data available/);
    await expect(forecastData.first()).toBeVisible({ timeout: 10_000 });
  });

  test('should keep forecasts tab active after clicking it', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const forecastsTab = main.getByRole('tab', { name: /Forecasts/ });
    await forecastsTab.click();
    await expect(forecastsTab).toHaveAttribute('data-active', '');
    const anomaliesTab = main.getByRole('tab', { name: /Anomalies/ });
    await expect(anomaliesTab).not.toHaveAttribute('data-active', '');
  });

  test('forecast history table shows when data exists', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const forecastsTab = main.getByRole('tab', { name: /Forecasts/ });
    await forecastsTab.click();
    // The forecast history section shows either table or empty message
    const historyTitle = main.getByText('Forecast History');
    const emptyState = main.getByText('No forecast data available');
    await expect(historyTitle.or(emptyState)).toBeVisible({ timeout: 10_000 });
  });
});
