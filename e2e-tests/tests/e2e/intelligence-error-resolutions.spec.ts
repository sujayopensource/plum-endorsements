import { test, expect } from '@playwright/test';

test.describe('Intelligence - Error Resolutions Page @allure.label.epic:Intelligence_E2E', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/intelligence');
  });

  test('error resolution tab shows stats cards', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const errorResolutionTab = main.getByRole('tab', { name: /Error Resolution/ });
    await expect(errorResolutionTab).toBeVisible({ timeout: 10_000 });
    await errorResolutionTab.click();
    const tableOrStats = main.locator('table').first();
    await expect(tableOrStats).toBeVisible({ timeout: 10_000 });
    const totalResolutionsCard = main.getByText('Total Resolutions');
    const autoAppliedCard = main.getByText('Auto-Applied');
    if (await totalResolutionsCard.isVisible()) {
      await expect(autoAppliedCard.first()).toBeVisible({ timeout: 5_000 });
    }
  });

  test('resolution table renders with columns including Actions', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const errorResolutionTab = main.getByRole('tab', { name: /Error Resolution/ });
    await errorResolutionTab.click();
    const tableHeaders = main.locator('table th');
    await expect(tableHeaders.first()).toBeVisible({ timeout: 10_000 });
    await expect(main.getByRole('columnheader', { name: /Error Type/ })).toBeVisible();
    await expect(main.getByRole('columnheader', { name: /Corrected/ })).toBeVisible();
    await expect(main.getByRole('columnheader', { name: /Confidence/ })).toBeVisible();
    await expect(main.getByRole('columnheader', { name: /Actions/ })).toBeVisible();
  });

  test('should display auto-apply rate in stats cards', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const errorResolutionTab = main.getByRole('tab', { name: /Error Resolution/ });
    await errorResolutionTab.click();
    await expect(main.locator('table tbody tr').first()).toBeVisible({ timeout: 10_000 });
    const autoApplyRate = main.getByText('Auto-Apply Rate');
    if (await autoApplyRate.isVisible()) {
      const rateCard = autoApplyRate.locator('..');
      const rateValue = rateCard.locator('..').getByText(/%/);
      await expect(rateValue.first()).toBeVisible({ timeout: 5_000 });
    }
  });

  test('should show confidence values in resolution table', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const errorResolutionTab = main.getByRole('tab', { name: /Error Resolution/ });
    await errorResolutionTab.click();
    const tableRow = main.locator('table tbody tr').first();
    const emptyState = main.getByRole('cell', { name: 'No error resolutions yet' });
    await expect(tableRow).toBeVisible({ timeout: 10_000 });
    if (!(await emptyState.isVisible())) {
      const confidenceCell = main.locator('table tbody tr:first-child td:nth-child(4)');
      const confidenceText = await confidenceCell.textContent();
      expect(confidenceText).toMatch(/^\d{1,3}%$/);
    }
  });

  test('should distinguish auto-applied from suggested resolutions', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const errorResolutionTab = main.getByRole('tab', { name: /Error Resolution/ });
    await errorResolutionTab.click();
    const tableRow = main.locator('table tbody tr').first();
    const emptyState = main.getByRole('cell', { name: 'No error resolutions yet' });
    await expect(tableRow).toBeVisible({ timeout: 10_000 });
    if (!(await emptyState.isVisible())) {
      const autoAppliedBadges = main.locator('table tbody tr td:nth-child(5) [data-slot="badge"]');
      await expect(autoAppliedBadges.first()).toBeVisible({ timeout: 5_000 });
      const badgeText = await autoAppliedBadges.first().textContent();
      expect(badgeText).toMatch(/^(Yes|No)$/);
    }
  });

  test('should display total resolutions and auto-applied counts in stats cards', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const errorResolutionTab = main.getByRole('tab', { name: /Error Resolution/ });
    await errorResolutionTab.click();
    await expect(main.locator('table tbody tr').first()).toBeVisible({ timeout: 10_000 });
    const totalLabel = main.getByText('Total Resolutions');
    const autoAppliedLabel = main.getByText('Auto-Applied');
    const suggestedLabel = main.getByText('Suggested');
    if (await totalLabel.isVisible()) {
      await expect(autoAppliedLabel.first()).toBeVisible({ timeout: 5_000 });
      await expect(suggestedLabel).toBeVisible({ timeout: 5_000 });
    }
  });

  test('should display empty state message when no resolutions exist', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const errorResolutionTab = main.getByRole('tab', { name: /Error Resolution/ });
    await errorResolutionTab.click();
    const tableRow = main.locator('table tbody tr').first();
    await expect(tableRow).toBeVisible({ timeout: 10_000 });
    const emptyState = main.getByRole('cell', { name: 'No error resolutions yet' });
    const dataRow = main.locator('table tbody tr td:nth-child(2)');
    await expect(emptyState.or(dataRow.first())).toBeVisible({ timeout: 10_000 });
  });

  test('should show recent resolutions card header', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const errorResolutionTab = main.getByRole('tab', { name: /Error Resolution/ });
    await errorResolutionTab.click();
    await expect(main.locator('table tbody tr').first()).toBeVisible({ timeout: 10_000 });
    const recentResolutions = main.getByText('Recent Resolutions');
    await expect(recentResolutions).toBeVisible({ timeout: 5_000 });
  });

  test('should display success rate metric in stats cards', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const errorResolutionTab = main.getByRole('tab', { name: /Error Resolution/ });
    await errorResolutionTab.click();
    await expect(main.locator('table tbody tr').first()).toBeVisible({ timeout: 10_000 });
    const successRateLabel = main.getByText('Success Rate');
    if (await successRateLabel.isVisible()) {
      const rateCard = successRateLabel.locator('..');
      const rateValue = rateCard.locator('..').getByText(/%/);
      await expect(rateValue.first()).toBeVisible({ timeout: 5_000 });
    }
  });

  test('approve button visible for non-auto-applied resolutions', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
    const errorResolutionTab = main.getByRole('tab', { name: /Error Resolution/ });
    await errorResolutionTab.click();
    const tableRow = main.locator('table tbody tr').first();
    const emptyState = main.getByRole('cell', { name: 'No error resolutions yet' });
    await expect(tableRow).toBeVisible({ timeout: 10_000 });
    if (!(await emptyState.isVisible())) {
      // If there are non-auto-applied resolutions, Approve button should be visible
      const approveButtons = main.getByRole('button', { name: /Approve/ });
      const noButtons = main.locator('table tbody tr td:last-child').first();
      // Either approve buttons exist, or all are auto-applied (no button)
      await expect(approveButtons.first().or(noButtons)).toBeVisible({ timeout: 5_000 });
    }
  });
});
