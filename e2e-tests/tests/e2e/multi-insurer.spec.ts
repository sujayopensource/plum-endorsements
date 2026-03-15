import { test, expect } from '@playwright/test';
import { createEndorsementViaApi, TEST_IDS, INSURER_IDS } from '../fixtures/test-helpers';

test.describe('Multi-Insurer Endorsements', () => {
  test('should create endorsement for ICICI Lombard insurer', async ({ page }) => {
    const endorsement = await createEndorsementViaApi({
      insurerId: INSURER_IDS.icici,
      policyId: 'cccccccc-cccc-cccc-cccc-cccccccccccc',
      employeeId: crypto.randomUUID(),
    });
    await page.goto(`/endorsements/${endorsement.id}`);
    await expect(page.getByText('Endorsement Detail')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('33333333')).toBeVisible();
  });

  test('should create endorsement for Bajaj Allianz insurer', async ({ page }) => {
    const endorsement = await createEndorsementViaApi({
      insurerId: INSURER_IDS.bajaj,
      employeeId: crypto.randomUUID(),
    });
    await page.goto(`/endorsements/${endorsement.id}`);
    await expect(page.getByText('Endorsement Detail')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('55555555')).toBeVisible();
  });

  test('should list endorsements from multiple insurers', async ({ page }) => {
    await createEndorsementViaApi({ insurerId: INSURER_IDS.icici, employeeId: crypto.randomUUID(), policyId: 'cccccccc-cccc-cccc-cccc-cccccccccccc' });
    await createEndorsementViaApi({ insurerId: INSURER_IDS.bajaj, employeeId: crypto.randomUUID() });
    await createEndorsementViaApi({ insurerId: INSURER_IDS.mock, employeeId: crypto.randomUUID() });

    await page.goto('/endorsements');
    // At least 3 endorsements should be visible (other tests may have created more)
    await expect(page.locator('table tbody tr').first()).toBeVisible({ timeout: 10000 });
    const rowCount = await page.locator('table tbody tr').count();
    expect(rowCount).toBeGreaterThanOrEqual(3);
  });

  test('should fetch insurer configurations via API', async ({ page }) => {
    const response = await page.request.get('http://localhost:8080/api/v1/insurers');
    expect(response.ok()).toBeTruthy();
    const insurers = await response.json();
    expect(insurers.length).toBeGreaterThanOrEqual(4);
  });
});
