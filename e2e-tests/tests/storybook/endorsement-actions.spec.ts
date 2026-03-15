import { test, expect } from '@playwright/test';

test.describe('EndorsementActions @allure.label.epic:Endorsement_E2E', () => {
  test('shows Submit button for PROVISIONALLY_COVERED status', async ({ page }) => {
    await page.goto('/iframe.html?id=endorsements-endorsementactions--submit-available&viewMode=story');
    await expect(page.getByText('Submit to Insurer').first()).toBeVisible();
  });

  test('shows Confirm and Reject buttons for SUBMITTED_REALTIME status', async ({ page }) => {
    await page.goto('/iframe.html?id=endorsements-endorsementactions--confirm-reject-available&viewMode=story');
    await expect(page.getByText('Confirm').first()).toBeVisible();
    await expect(page.getByText('Reject').first()).toBeVisible();
  });

  test('shows Retry button for REJECTED status with retryCount < 3', async ({ page }) => {
    await page.goto('/iframe.html?id=endorsements-endorsementactions--retry-available&viewMode=story');
    await expect(page.getByText('Retry Submission').first()).toBeVisible();
  });

  test('shows terminal state message for CONFIRMED', async ({ page }) => {
    await page.goto('/iframe.html?id=endorsements-endorsementactions--terminal-confirmed&viewMode=story');
    await expect(page.getByText('terminal state')).toBeVisible();
  });

  test('shows terminal state message for FAILED_PERMANENT', async ({ page }) => {
    await page.goto('/iframe.html?id=endorsements-endorsementactions--terminal-failed&viewMode=story');
    await expect(page.getByText('terminal state')).toBeVisible();
  });
});
