import { test, expect } from '@playwright/test';
import { createEndorsementViaApi } from '../fixtures/test-helpers';

const API_BASE = 'http://localhost:8080/api/v1';

test.describe('Audit Logging @allure.label.epic:Endorsement_E2E', () => {
  test('audit logs API returns paginated response', async ({ page }) => {
    const response = await page.request.get(`${API_BASE}/audit-logs`);
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(body.content).toBeDefined();
    expect(body.totalElements).toBeDefined();
    expect(body.totalPages).toBeDefined();
    expect(body.size).toBeDefined();
    expect(body.number).toBeDefined();
  });

  test('creating an endorsement generates an audit log entry', async ({ page }) => {
    // Create an endorsement to generate an audit log
    await createEndorsementViaApi({ employeeId: crypto.randomUUID() });

    // Query audit logs
    const response = await page.request.get(`${API_BASE}/audit-logs`);
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(body.totalElements).toBeGreaterThanOrEqual(1);

    // Verify audit log entry structure
    if (body.content.length > 0) {
      const entry = body.content[0];
      expect(entry.id).toBeDefined();
      expect(entry.action).toBeDefined();
      expect(entry.createdAt).toBeDefined();
    }
  });

  test('audit logs can be filtered by entity type', async ({ page }) => {
    const response = await page.request.get(
      `${API_BASE}/audit-logs?entityType=Endorsement`,
    );
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    // All entries should be for the Endorsement entity type
    for (const entry of body.content) {
      expect(entry.entityType).toBe('Endorsement');
    }
  });

  test('audit logs support pagination with page and size params', async ({ page }) => {
    const response = await page.request.get(`${API_BASE}/audit-logs?page=0&size=5`);
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(body.size).toBe(5);
    expect(body.number).toBe(0);
    expect(body.content.length).toBeLessThanOrEqual(5);
  });

  test('audit log entries have valid timestamps', async ({ page }) => {
    // Query existing audit logs (other tests already create endorsements that generate audit entries)
    const response = await page.request.get(`${API_BASE}/audit-logs`);
    expect(response.ok()).toBeTruthy();
    const body = await response.json();

    for (const entry of body.content) {
      const createdAt = new Date(entry.createdAt);
      expect(createdAt.getTime()).not.toBeNaN();
      // Timestamp should be a valid ISO date (not in the future)
      expect(createdAt.getTime()).toBeLessThanOrEqual(Date.now() + 60_000);
    }
  });
});
