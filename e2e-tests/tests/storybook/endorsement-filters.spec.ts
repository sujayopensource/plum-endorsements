import { test, expect } from '@playwright/test';

test.describe('EndorsementFilters @allure.label.epic:Endorsement_E2E', () => {
  test('renders employer ID input and status filter button', async ({ page }) => {
    await page.goto('/iframe.html?id=endorsements-endorsementfilters--default&viewMode=story');
    const input = page.getByPlaceholder('Employer ID (UUID)');
    await expect(input).toBeVisible();
    await expect(input).toHaveValue('11111111-1111-1111-1111-111111111111');
    await expect(page.getByText('Status').first()).toBeVisible();
  });

  test('shows selected status count badge', async ({ page }) => {
    await page.goto('/iframe.html?id=endorsements-endorsementfilters--with-selected-statuses&viewMode=story');
    // Should show "2" badge for 2 selected statuses (CONFIRMED, REJECTED)
    await expect(page.getByText('2')).toBeVisible();
  });

  test('status filter popover opens on click', async ({ page }) => {
    await page.goto('/iframe.html?id=endorsements-endorsementfilters--default&viewMode=story');
    await page.getByText('Status').first().click();
    // Popover should show status options
    await expect(page.getByText('Created')).toBeVisible();
    await expect(page.getByText('Confirmed')).toBeVisible();
  });
});
