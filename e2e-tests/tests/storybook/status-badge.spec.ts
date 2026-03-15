import { test, expect } from '@playwright/test';

const STATUSES = [
  { story: 'created', label: 'Created' },
  { story: 'validated', label: 'Validated' },
  { story: 'provisional-covered', label: 'Prov. Covered' },
  { story: 'submitted-realtime', label: 'Submitted (RT)' },
  { story: 'queued-for-batch', label: 'Queued (Batch)' },
  { story: 'batch-submitted', label: 'Batch Submitted' },
  { story: 'insurer-processing', label: 'Processing' },
  { story: 'confirmed', label: 'Confirmed' },
  { story: 'rejected', label: 'Rejected' },
  { story: 'retry-pending', label: 'Retry Pending' },
  { story: 'failed-permanent', label: 'Failed' },
];

test.describe('StatusBadge @allure.label.epic:Endorsement_E2E', () => {
  for (const { story, label } of STATUSES) {
    test(`renders ${label} status badge`, async ({ page }) => {
      await page.goto(`/iframe.html?id=shared-statusbadge--${story}&viewMode=story`);
      const badge = page.locator('[data-slot="badge"]');
      await expect(badge).toBeVisible();
      await expect(badge).toContainText(label);
    });
  }

  test('all status badges render without errors', async ({ page }) => {
    for (const { story, label } of STATUSES) {
      await page.goto(`/iframe.html?id=shared-statusbadge--${story}&viewMode=story`);
      const badge = page.locator('[data-slot="badge"]');
      await expect(badge).toBeVisible();
      await expect(badge).toContainText(label);
    }
  });
});
