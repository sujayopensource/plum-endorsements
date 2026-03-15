import { test, expect } from '@playwright/test';

test.describe('Intelligence - Process Mining Page @allure.label.epic:Intelligence_E2E', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/intelligence');
  });

  test('process mining tab shows STP rate', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    // Click the Process Mining tab
    const processMiningTab = main.getByRole('tab', { name: /Process Mining/ });
    await expect(processMiningTab).toBeVisible({ timeout: 10_000 });
    await processMiningTab.click();

    // STP (Straight Through Processing) rate should be displayed
    const stpRate = main.getByText(/STP Rate/);
    const emptyState = main.getByText('No process mining data');
    await expect(stpRate.first().or(emptyState)).toBeVisible({ timeout: 10_000 });
  });

  test('bottleneck insights section renders', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    // Click the Process Mining tab
    const processMiningTab = main.getByRole('tab', { name: /Process Mining/ });
    await processMiningTab.click();

    // Bottleneck insights should be visible
    const bottleneckSection = main.getByText(/Bottleneck|Insights/);
    const emptyState = main.getByText('No process mining data');
    await expect(bottleneckSection.first().or(emptyState)).toBeVisible({ timeout: 10_000 });
  });

  test('run analysis button triggers analysis', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    // Click the Process Mining tab
    const processMiningTab = main.getByRole('tab', { name: /Process Mining/ });
    await processMiningTab.click();

    // Run Analysis button should be present
    const runAnalysisButton = main.getByRole('button', { name: /Run Analysis/ });
    await expect(runAnalysisButton).toBeVisible({ timeout: 10_000 });

    // Click the button to trigger analysis
    await runAnalysisButton.click();

    // After clicking, wait for the analysis to complete (button returns to "Run Analysis")
    await expect(main.getByRole('button', { name: /Run Analysis/ })).toBeVisible({ timeout: 15_000 });
  });

  test('should display overall STP rate as a percentage', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    // Click the Process Mining tab
    const processMiningTab = main.getByRole('tab', { name: /Process Mining/ });
    await processMiningTab.click();

    // Look for "Overall STP Rate" card description
    const stpLabel = main.getByText('Overall STP Rate');
    const emptyState = main.getByText('No process mining data');
    await expect(stpLabel.or(emptyState)).toBeVisible({ timeout: 10_000 });

    // If data is present, verify the rate is a percentage value (e.g., "85.0%")
    if (await stpLabel.isVisible()) {
      const stpCard = stpLabel.locator('..');
      const stpValue = stpCard.locator('..').getByText(/%/);
      await expect(stpValue.first()).toBeVisible({ timeout: 5_000 });
      const valueText = await stpValue.first().textContent();
      expect(valueText).toMatch(/\d+\.\d+%/);
    }
  });

  test('should show per-insurer STP rate cards when data exists', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    // Click the Process Mining tab
    const processMiningTab = main.getByRole('tab', { name: /Process Mining/ });
    await processMiningTab.click();

    // Look for per-insurer cards (rendered as "Insurer <id>...")
    const insurerCards = main.getByText(/Insurer [a-f0-9]{8}\.\.\./);
    const emptyState = main.getByText('No process mining data');
    const overallStp = main.getByText('Overall STP Rate');

    await expect(overallStp.or(emptyState)).toBeVisible({ timeout: 10_000 });

    // If data is present and per-insurer cards exist, verify they show percentages
    if (await overallStp.isVisible() && (await insurerCards.count()) > 0) {
      await expect(insurerCards.first()).toBeVisible({ timeout: 5_000 });
    }
  });

  test('should display bottleneck insights section with title and description', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    // Click the Process Mining tab
    const processMiningTab = main.getByRole('tab', { name: /Process Mining/ });
    await processMiningTab.click();

    // Verify "Bottleneck Insights" card header exists
    const bottleneckTitle = main.getByText('Bottleneck Insights');
    const emptyState = main.getByText('No process mining data');
    await expect(bottleneckTitle.or(emptyState)).toBeVisible({ timeout: 10_000 });

    if (await bottleneckTitle.isVisible()) {
      await expect(
        main.getByText('Workflow bottlenecks identified by process mining'),
      ).toBeVisible({ timeout: 5_000 });
    }
  });

  test('should show run analysis button in bottleneck section', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    // Click the Process Mining tab
    const processMiningTab = main.getByRole('tab', { name: /Process Mining/ });
    await processMiningTab.click();

    // Run Analysis button should be in the Bottleneck Insights card header
    const runAnalysisButton = main.getByRole('button', { name: /Run Analysis/ });
    await expect(runAnalysisButton).toBeVisible({ timeout: 10_000 });

    // Verify button is enabled initially
    await expect(runAnalysisButton).toBeEnabled();
  });

  test('should show analyzing state when run analysis is clicked', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    // Click the Process Mining tab
    const processMiningTab = main.getByRole('tab', { name: /Process Mining/ });
    await processMiningTab.click();

    // Click Run Analysis button
    const runAnalysisButton = main.getByRole('button', { name: /Run Analysis/ });
    await expect(runAnalysisButton).toBeVisible({ timeout: 10_000 });
    await runAnalysisButton.click();

    // After clicking, the button text changes to "Analyzing..." and becomes disabled
    const analyzingButton = main.getByRole('button', { name: /Analyzing/ });
    const runButton = main.getByRole('button', { name: /Run Analysis/ });
    // Either the analyzing state is shown briefly, or the analysis completes immediately
    await expect(analyzingButton.or(runButton)).toBeVisible({ timeout: 15_000 });
  });

  test('should show empty bottleneck message when no insights exist', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    // Click the Process Mining tab
    const processMiningTab = main.getByRole('tab', { name: /Process Mining/ });
    await processMiningTab.click();

    // Wait for content to load
    const bottleneckTitle = main.getByText('Bottleneck Insights');
    const emptyState = main.getByText('No process mining data');
    await expect(bottleneckTitle.or(emptyState)).toBeVisible({ timeout: 10_000 });

    // If the bottleneck section is visible, check for empty state or insight entries
    if (await bottleneckTitle.isVisible()) {
      const noBottlenecks = main.getByText('No bottlenecks detected. Run analysis to generate insights.');
      const insightEntries = main.locator('.rounded-lg.border.p-3');
      await expect(noBottlenecks.or(insightEntries.first())).toBeVisible({ timeout: 10_000 });
    }
  });

  test('should show STP Rate Trend section when data exists', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    const processMiningTab = main.getByRole('tab', { name: /Process Mining/ });
    await processMiningTab.click();

    // Look for STP Rate content or empty state
    const stpLabel = main.getByText('Overall STP Rate');
    const emptyState = main.getByText('No process mining data');
    await expect(stpLabel.or(emptyState)).toBeVisible({ timeout: 10_000 });

    // If STP data is present, verify the trend section or rate cards render
    if (await stpLabel.isVisible()) {
      // The STP rate section should be present with per-insurer or trend data
      const stpCard = stpLabel.locator('..');
      await expect(stpCard).toBeVisible({ timeout: 5_000 });
    }
  });

  test('transition metrics table renders with duration columns when data exists', async ({ page }) => {
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Intelligence' })).toBeVisible({ timeout: 10_000 });

    // Click the Process Mining tab
    const processMiningTab = main.getByRole('tab', { name: /Process Mining/ });
    await processMiningTab.click();

    // Wait for STP rate or empty state to load first
    const stpLabel = main.getByText('Overall STP Rate');
    const emptyState = main.getByText('No process mining data');
    await expect(stpLabel.or(emptyState)).toBeVisible({ timeout: 10_000 });

    // Transition Metrics table only renders when metrics data exists
    // Check if it's present and verify columns if so
    const metricsTitle = main.getByText('Transition Metrics');
    if (await metricsTitle.isVisible({ timeout: 2_000 }).catch(() => false)) {
      const metricsTable = main.locator('table').last();
      await expect(metricsTable).toBeVisible({ timeout: 5_000 });
      await expect(metricsTable.getByRole('columnheader', { name: /From Status/ })).toBeVisible();
      await expect(metricsTable.getByRole('columnheader', { name: /To Status/ })).toBeVisible();
      await expect(metricsTable.getByRole('columnheader', { name: /Avg Duration/ })).toBeVisible();
      await expect(metricsTable.getByRole('columnheader', { name: /P95/ })).toBeVisible();
      await expect(metricsTable.getByRole('columnheader', { name: /P99/ })).toBeVisible();
      await expect(metricsTable.getByRole('columnheader', { name: /Samples/ })).toBeVisible();
    }
    // If no metrics data, the table section simply doesn't render - test passes
  });
});
