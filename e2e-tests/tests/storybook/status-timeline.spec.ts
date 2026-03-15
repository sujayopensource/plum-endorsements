import { test, expect } from '@playwright/test';

test.describe('StatusTimeline @allure.label.epic:Endorsement_E2E', () => {
  test('renders realtime path steps for CREATED status', async ({ page }) => {
    await page.goto('/iframe.html?id=endorsements-statustimeline--realtime-created&viewMode=story');
    await expect(page.getByText('Created')).toBeVisible();
    await expect(page.getByText('Validated')).toBeVisible();
    await expect(page.getByText('Prov. Covered')).toBeVisible();
    await expect(page.getByText('Submitted (RT)')).toBeVisible();
    await expect(page.getByText('Confirmed')).toBeVisible();
  });

  test('renders realtime path fully completed for CONFIRMED status', async ({ page }) => {
    await page.goto('/iframe.html?id=endorsements-statustimeline--realtime-confirmed&viewMode=story');
    await expect(page.getByText('Confirmed')).toBeVisible();
  });

  test('renders batch path with batch-specific steps', async ({ page }) => {
    await page.goto('/iframe.html?id=endorsements-statustimeline--batch-path&viewMode=story');
    await expect(page.getByText('Queued')).toBeVisible();
    await expect(page.getByText('Batch Sent')).toBeVisible();
  });

  test('shows rejection error banner with reason', async ({ page }) => {
    await page.goto('/iframe.html?id=endorsements-statustimeline--rejected&viewMode=story');
    await expect(page.getByText('Document verification failed')).toBeVisible();
    await expect(page.getByText(/Rejected.*attempt.*1\/3/)).toBeVisible();
  });

  test('shows permanent failure error banner', async ({ page }) => {
    await page.goto('/iframe.html?id=endorsements-statustimeline--failed-permanent&viewMode=story');
    await expect(page.getByText('Permanently Failed')).toBeVisible();
    await expect(page.getByText('Maximum retries exceeded')).toBeVisible();
  });
});
