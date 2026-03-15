import { test, expect } from '@playwright/test';

const TYPES = [
  { story: 'add', label: 'Add' },
  { story: 'delete', label: 'Delete' },
  { story: 'update', label: 'Update' },
];

test.describe('TypeBadge @allure.label.epic:Endorsement_E2E', () => {
  for (const { story, label } of TYPES) {
    test(`renders ${label} type badge`, async ({ page }) => {
      await page.goto(`/iframe.html?id=shared-typebadge--${story}&viewMode=story`);
      await expect(page.getByText(label, { exact: true }).last()).toBeVisible();
    });
  }
});
