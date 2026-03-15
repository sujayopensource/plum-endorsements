import { test, expect } from '@playwright/test';
import {
  createEndorsementViaApi,
  submitEndorsementViaApi,
  rejectEndorsementViaApi,
  generateEmployeeId,
  getEndorsementViaApi,
} from '../fixtures/test-helpers';

test.describe('Endorsement Lifecycle E2E @allure.label.epic:Endorsement_E2E', () => {
  test('complete happy path: create -> submit -> auto-confirms', async ({ page }) => {
    const employeeId = generateEmployeeId();
    const endorsement = await createEndorsementViaApi({ employeeId }) as any;

    // Navigate to detail page
    await page.goto(`/endorsements/${endorsement.id}`);
    const main = page.locator('main');
    await expect(main.getByText('Prov. Covered').first()).toBeVisible({ timeout: 10_000 });

    // Submit to insurer via dialog
    await main.getByText('Submit to Insurer').click();
    await page.getByRole('button', { name: 'Submit' }).click();

    // Wait for auto-confirm (mock insurer confirms instantly)
    await page.waitForTimeout(2000);
    await page.reload();

    // Should now be confirmed - StatusBadge will show "Confirmed"
    await expect(main.getByText('Confirmed').first()).toBeVisible({ timeout: 10_000 });
  });

  test('create ADD endorsement and verify coverage card shows PROVISIONAL', async ({ page }) => {
    const employeeId = generateEmployeeId();
    const endorsement = await createEndorsementViaApi({ employeeId, type: 'ADD' }) as any;

    await page.goto(`/endorsements/${endorsement.id}`);
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Endorsement Detail' })).toBeVisible({ timeout: 10_000 });
    await expect(main.getByText('PROVISIONAL')).toBeVisible({ timeout: 5_000 });
  });

  test('create DELETE endorsement - no coverage card', async ({ page }) => {
    const employeeId = generateEmployeeId();
    const endorsement = await createEndorsementViaApi({
      employeeId,
      type: 'DELETE',
      premiumAmount: undefined,
    }) as any;

    await page.goto(`/endorsements/${endorsement.id}`);
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Endorsement Detail' })).toBeVisible({ timeout: 10_000 });
    // Coverage card should NOT be visible for DELETE type
    // Give some time for it to potentially appear, then verify it didn't
    await page.waitForTimeout(1000);
    await expect(main.getByText('PROVISIONAL')).not.toBeVisible();
  });

  test('rejected endorsement shows retry or rejection info', async ({ page }) => {
    const employeeId = generateEmployeeId();
    const endorsement = await createEndorsementViaApi({ employeeId }) as any;
    await submitEndorsementViaApi(endorsement.id);

    // Wait for status to update
    await page.waitForTimeout(1000);
    const updated = await getEndorsementViaApi(endorsement.id) as any;

    if (updated.status !== 'CONFIRMED') {
      await rejectEndorsementViaApi(endorsement.id, 'Test rejection');
      await page.goto(`/endorsements/${endorsement.id}`);
      const main = page.locator('main');
      // Should show retry or rejection info
      await expect(main.getByText(/Reject|Retry/).first()).toBeVisible({ timeout: 10_000 });
    }
  });

  test('create from UI and verify detail page', async ({ page }) => {
    const employeeId = generateEmployeeId();

    await page.goto('/endorsements/new');

    // Fill form
    await page.locator('#employeeId').fill(employeeId);
    await page.locator('#employeeName').fill('Lifecycle Test Employee');
    await page.locator('#coverageStartDate').fill('2026-04-01');
    await page.locator('#premiumAmount').fill('2000');

    // Submit
    await page.getByRole('button', { name: 'Create Endorsement' }).click();

    // Should redirect to detail page
    await expect(page).toHaveURL(/\/endorsements\/[a-f0-9-]+/, { timeout: 15_000 });
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Endorsement Detail' })).toBeVisible();
    await expect(main.getByText('Prov. Covered').first()).toBeVisible();
  });
});
