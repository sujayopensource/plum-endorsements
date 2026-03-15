import { test, expect } from '@playwright/test';
import { createEndorsementViaApi, submitEndorsementViaApi, generateEmployeeId } from '../fixtures/test-helpers';

test.describe('Endorsement Detail Page @allure.label.epic:Endorsement_E2E', () => {
  test('displays endorsement details after creation', async ({ page }) => {
    const employeeId = generateEmployeeId();
    const endorsement = await createEndorsementViaApi({ employeeId }) as any;

    await page.goto(`/endorsements/${endorsement.id}`);
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Endorsement Detail' })).toBeVisible({ timeout: 10_000 });
    // Details card with detail rows
    await expect(main.getByText('Details').first()).toBeVisible();
    await expect(main.getByText('Type').first()).toBeVisible();
    await expect(main.getByText('Coverage Period')).toBeVisible();
  });

  test('shows status timeline for realtime path', async ({ page }) => {
    const employeeId = generateEmployeeId();
    const endorsement = await createEndorsementViaApi({ employeeId }) as any;

    await page.goto(`/endorsements/${endorsement.id}`);
    const main = page.locator('main');
    // Timeline steps - use first() since "Created" also appears as a detail row label
    await expect(main.getByText('Created').first()).toBeVisible({ timeout: 10_000 });
    await expect(main.getByText('Validated').first()).toBeVisible();
    await expect(main.getByText('Prov. Covered').first()).toBeVisible();
  });

  test('shows Submit to Insurer action for provisionally covered endorsement', async ({ page }) => {
    const employeeId = generateEmployeeId();
    const endorsement = await createEndorsementViaApi({ employeeId }) as any;

    await page.goto(`/endorsements/${endorsement.id}`);
    const main = page.locator('main');
    await expect(main.getByText('Submit to Insurer')).toBeVisible({ timeout: 10_000 });
  });

  test('submit dialog opens and can be cancelled', async ({ page }) => {
    const employeeId = generateEmployeeId();
    const endorsement = await createEndorsementViaApi({ employeeId }) as any;

    await page.goto(`/endorsements/${endorsement.id}`);
    const main = page.locator('main');
    await main.getByText('Submit to Insurer').click({ timeout: 10_000 });
    await expect(page.getByText('This will submit the endorsement')).toBeVisible();
    await page.getByRole('button', { name: 'Cancel' }).click();
  });

  test('coverage card shows for ADD endorsement', async ({ page }) => {
    const employeeId = generateEmployeeId();
    const endorsement = await createEndorsementViaApi({ employeeId }) as any;

    await page.goto(`/endorsements/${endorsement.id}`);
    const main = page.locator('main');
    // Wait for page to load
    await expect(main.locator('h1', { hasText: 'Endorsement Detail' })).toBeVisible({ timeout: 10_000 });
    // Coverage card - use card title specifically (not "Coverage Period")
    await expect(main.getByText('PROVISIONAL')).toBeVisible({ timeout: 5_000 });
  });

  test('back button navigates to endorsements list', async ({ page }) => {
    const employeeId = generateEmployeeId();
    const endorsement = await createEndorsementViaApi({ employeeId }) as any;

    await page.goto(`/endorsements/${endorsement.id}`);
    const main = page.locator('main');
    await expect(main.locator('h1', { hasText: 'Endorsement Detail' })).toBeVisible({ timeout: 10_000 });
    // The back button is a Link wrapping an ArrowLeft icon
    const backLink = main.getByRole('link').filter({ has: page.locator('svg') }).first();
    await backLink.click();
    await expect(page).toHaveURL(/\/endorsements$/);
  });
});
