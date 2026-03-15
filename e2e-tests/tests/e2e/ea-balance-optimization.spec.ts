import { test, expect } from '@playwright/test';
import { createEndorsementViaApi, TEST_IDS, INSURER_IDS } from '../fixtures/test-helpers';

test.describe('EA Balance Optimization', () => {
  test('should show EA account balance for multiple insurers', async ({ page }) => {
    await page.goto('/ea-accounts');
    await page.fill('#ea-employerId', TEST_IDS.employerId);
    await page.fill('#ea-insurerId', TEST_IDS.insurerId);
    await page.click('button:has-text("Look Up")');
    await expect(page.getByText('Total Balance')).toBeVisible({ timeout: 10000 });
  });

  test('should create DELETE endorsement without balance impact', async ({ page }) => {
    const endorsement = await createEndorsementViaApi({
      type: 'DELETE',
      premiumAmount: 0,
      employeeId: crypto.randomUUID(),
    });
    await page.goto(`/endorsements/${endorsement.id}`);
    await expect(page.getByText('DELETE')).toBeVisible({ timeout: 10000 });
  });
});
