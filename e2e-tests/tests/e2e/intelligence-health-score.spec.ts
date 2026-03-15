import { test, expect } from '@playwright/test';
import { TEST_IDS } from '../fixtures/test-helpers';

const API_BASE = 'http://localhost:8080/api/v1';

test.describe('Intelligence - Employer Health Score @allure.label.epic:Intelligence_E2E', () => {
  test('health score API returns valid response for standard employer', async ({ page }) => {
    const response = await page.request.get(
      `${API_BASE}/intelligence/employers/${TEST_IDS.employerId}/health-score`,
    );
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(body.overallScore).toBeGreaterThanOrEqual(0);
    expect(body.overallScore).toBeLessThanOrEqual(100);
    expect(['LOW', 'MEDIUM', 'HIGH']).toContain(body.riskLevel);
    expect(body.endorsementSuccessRate).toBeDefined();
    expect(body.anomalyScore).toBeDefined();
    expect(body.balanceHealthScore).toBeDefined();
    expect(body.reconciliationScore).toBeDefined();
    expect(body.calculatedAt).toBeDefined();
  });

  test('health score API returns 200 for employer with no data', async ({ page }) => {
    const emptyEmployerId = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee';
    const response = await page.request.get(
      `${API_BASE}/intelligence/employers/${emptyEmployerId}/health-score`,
    );
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    // Employer with no endorsements should get a perfect default score
    expect(body.overallScore).toBeGreaterThanOrEqual(0);
    expect(body.riskLevel).toBeDefined();
  });

  test('health score contains all required components', async ({ page }) => {
    const response = await page.request.get(
      `${API_BASE}/intelligence/employers/${TEST_IDS.employerId}/health-score`,
    );
    const body = await response.json();

    // Verify all component scores are between 0 and 100
    expect(body.endorsementSuccessRate).toBeGreaterThanOrEqual(0);
    expect(body.endorsementSuccessRate).toBeLessThanOrEqual(100);
    expect(body.anomalyScore).toBeGreaterThanOrEqual(0);
    expect(body.anomalyScore).toBeLessThanOrEqual(100);
    expect(body.balanceHealthScore).toBeGreaterThanOrEqual(0);
    expect(body.balanceHealthScore).toBeLessThanOrEqual(100);
    expect(body.reconciliationScore).toBeGreaterThanOrEqual(0);
    expect(body.reconciliationScore).toBeLessThanOrEqual(100);
  });

  test('health score calculatedAt is recent', async ({ page }) => {
    const response = await page.request.get(
      `${API_BASE}/intelligence/employers/${TEST_IDS.employerId}/health-score`,
    );
    const body = await response.json();

    const calculatedAt = new Date(body.calculatedAt);
    const now = new Date();
    const diffMs = now.getTime() - calculatedAt.getTime();
    // Should be calculated within the last 5 seconds
    expect(diffMs).toBeLessThan(5000);
  });
});
