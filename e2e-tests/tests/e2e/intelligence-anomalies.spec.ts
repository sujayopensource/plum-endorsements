import { test, expect } from '@playwright/test';

test.describe('Intelligence - Anomalies Page @allure.label.epic:Intelligence_E2E', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/intelligence');
  });

  test('intelligence page loads with correct title', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });
  });

  test('anomalies tab shows table with headers', async ({ page }) => {
    const main = page.locator('main');
    // Anomalies tab should be the default or click it explicitly
    const anomaliesTab = main.getByRole('tab', { name: /Anomalies/ });
    await expect(anomaliesTab).toBeVisible({ timeout: 10_000 });
    await anomaliesTab.click();

    // Wait for the anomalies table to render
    await expect(main.getByRole('columnheader', { name: /Employer/ })).toBeVisible({ timeout: 10_000 });
    await expect(main.getByRole('columnheader', { name: /Type/ })).toBeVisible();
    await expect(main.getByRole('columnheader', { name: /Score/ })).toBeVisible();
    await expect(main.getByRole('columnheader', { name: /Status/ })).toBeVisible();
    await expect(main.getByRole('columnheader', { name: /Actions/ })).toBeVisible();
  });

  test('review action buttons visible for flagged anomalies', async ({ page }) => {
    const main = page.locator('main');
    // Navigate to Anomalies tab
    const anomaliesTab = main.getByRole('tab', { name: /Anomalies/ });
    await expect(anomaliesTab).toBeVisible({ timeout: 10_000 });
    await anomaliesTab.click();

    // Wait for table to load
    await expect(main.locator('table tbody tr').first()).toBeVisible({ timeout: 10_000 });

    // Flagged anomalies should have a Review button
    const reviewButtons = main.getByRole('button', { name: /Review/ });
    const noAnomalies = main.getByRole('cell', { name: 'No anomalies detected' });
    // Either review buttons exist or the empty state is shown
    await expect(reviewButtons.first().or(noAnomalies)).toBeVisible({ timeout: 10_000 });
  });

  test('should display anomaly score with visual indicator', async ({ page }) => {
    const main = page.locator('main');
    const anomaliesTab = main.getByRole('tab', { name: /Anomalies/ });
    await expect(anomaliesTab).toBeVisible({ timeout: 10_000 });
    await anomaliesTab.click();

    // Wait for table to load (first <tr> is always present — data rows or empty state)
    const tableRow = main.locator('table tbody tr').first();
    const noAnomalies = main.getByRole('cell', { name: 'No anomalies detected' });
    await expect(tableRow).toBeVisible({ timeout: 10_000 });

    // If anomalies exist, verify score badges are present with percentage values
    if (!(await noAnomalies.isVisible())) {
      // Score column renders an AnomalyScoreBadge with percentage text (e.g., "85%")
      const scoreBadges = main.locator('table tbody tr td:nth-child(3) [data-slot="badge"]');
      await expect(scoreBadges.first()).toBeVisible({ timeout: 5_000 });

      // Verify the score text matches a percentage pattern (0-100%)
      const scoreText = await scoreBadges.first().textContent();
      expect(scoreText).toMatch(/^\d{1,3}%$/);
    }
  });

  test('should show anomaly detail columns when anomalies exist', async ({ page }) => {
    const main = page.locator('main');
    const anomaliesTab = main.getByRole('tab', { name: /Anomalies/ });
    await expect(anomaliesTab).toBeVisible({ timeout: 10_000 });
    await anomaliesTab.click();

    // Wait for table to load (first <tr> is always present — data rows or empty state)
    const tableRow = main.locator('table tbody tr').first();
    const noAnomalies = main.getByRole('cell', { name: 'No anomalies detected' });
    await expect(tableRow).toBeVisible({ timeout: 10_000 });

    // If anomalies exist, verify detail columns: Type, Score, Explanation, Status
    if (!(await noAnomalies.isVisible())) {
      // Type column has a Badge with anomaly type
      const typeBadge = main.locator('table tbody tr:first-child td:nth-child(2) [data-slot="badge"]');
      await expect(typeBadge).toBeVisible({ timeout: 5_000 });

      // Explanation column has descriptive text
      const explanationCell = main.locator('table tbody tr:first-child td:nth-child(4)');
      const explanationText = await explanationCell.textContent();
      expect(explanationText?.trim().length).toBeGreaterThan(0);

      // Status column has a status badge
      const statusBadge = main.locator('table tbody tr:first-child td:nth-child(5) [data-slot="badge"]');
      await expect(statusBadge).toBeVisible({ timeout: 5_000 });
    }
  });

  test('should display empty state message when no anomalies exist', async ({ page }) => {
    const main = page.locator('main');
    const anomaliesTab = main.getByRole('tab', { name: /Anomalies/ });
    await expect(anomaliesTab).toBeVisible({ timeout: 10_000 });
    await anomaliesTab.click();

    // Wait for table to load (first <tr> is always present — data rows or empty state)
    const tableRow = main.locator('table tbody tr').first();
    const noAnomalies = main.getByRole('cell', { name: 'No anomalies detected' });
    await expect(tableRow).toBeVisible({ timeout: 10_000 });

    // If empty state is shown, verify the message text
    if (await noAnomalies.isVisible()) {
      await expect(noAnomalies).toContainText('No anomalies detected');
    }
  });

  test('should show review and dismiss buttons for flagged anomaly rows', async ({ page }) => {
    const main = page.locator('main');
    const anomaliesTab = main.getByRole('tab', { name: /Anomalies/ });
    await expect(anomaliesTab).toBeVisible({ timeout: 10_000 });
    await anomaliesTab.click();

    // Wait for table to load (first <tr> is always present — data rows or empty state)
    const tableRow = main.locator('table tbody tr').first();
    const noAnomalies = main.getByRole('cell', { name: 'No anomalies detected' });
    await expect(tableRow).toBeVisible({ timeout: 10_000 });

    // If anomalies exist, look for FLAGGED status rows with Review and Dismiss buttons
    if (!(await noAnomalies.isVisible())) {
      const flaggedBadge = main.locator('[data-slot="badge"]', { hasText: 'FLAGGED' });
      if (await flaggedBadge.first().isVisible()) {
        // Flagged rows should have both Review and Dismiss action buttons
        const reviewButton = main.getByRole('button', { name: /Review/ });
        const dismissButton = main.getByRole('button', { name: /Dismiss/ });
        await expect(reviewButton.first()).toBeVisible({ timeout: 5_000 });
        await expect(dismissButton.first()).toBeVisible({ timeout: 5_000 });
      }
    }
  });

  test('should display DORMANCY_BREAK anomaly type in table when present', async ({ page }) => {
    const main = page.locator('main');
    const anomaliesTab = main.getByRole('tab', { name: /Anomalies/ });
    await expect(anomaliesTab).toBeVisible({ timeout: 10_000 });
    await anomaliesTab.click();

    const tableRow = main.locator('table tbody tr').first();
    const noAnomalies = main.getByRole('cell', { name: 'No anomalies detected' });
    await expect(tableRow).toBeVisible({ timeout: 10_000 });

    // If anomalies exist, check that the type column can include DORMANCY_BREAK
    if (!(await noAnomalies.isVisible())) {
      const typeBadges = main.locator('table tbody tr td:nth-child(2) [data-slot="badge"]');
      await expect(typeBadges.first()).toBeVisible({ timeout: 5_000 });
      // Verify all type badges have valid anomaly type text
      const count = await typeBadges.count();
      for (let i = 0; i < count; i++) {
        const text = await typeBadges.nth(i).textContent();
        expect(text).toMatch(/^(VOLUME SPIKE|ADD DELETE CYCLING|SUSPICIOUS TIMING|UNUSUAL PREMIUM|PREMIUM SPIKE|DORMANCY BREAK)$/);
      }
    }
  });

  test('should show card header with title and description', async ({ page }) => {
    const main = page.locator('main');
    const anomaliesTab = main.getByRole('tab', { name: /Anomalies/ });
    await expect(anomaliesTab).toBeVisible({ timeout: 10_000 });
    await anomaliesTab.click();

    // Verify the card header for the anomalies section
    await expect(main.getByText('Flagged Anomalies')).toBeVisible({ timeout: 10_000 });
    await expect(main.getByText('Endorsements flagged by the anomaly detection engine')).toBeVisible();
  });
});
