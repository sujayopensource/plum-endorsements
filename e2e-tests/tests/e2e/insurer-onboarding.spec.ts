import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080/api/v1';

test.describe('Self-Service Insurer Onboarding @allure.label.epic:Endorsement_E2E', () => {
  test('create a new insurer configuration', async ({ page }) => {
    const uniqueCode = `TEST_${Date.now()}`;
    const response = await page.request.post(`${API_BASE}/insurers`, {
      data: {
        insurerName: `Test Insurer ${uniqueCode}`,
        insurerCode: uniqueCode,
        adapterType: 'MOCK',
        supportsRealTime: true,
        supportsBatch: false,
        maxBatchSize: 50,
        batchSlaHours: 24,
        rateLimitPerMinute: 30,
      },
    });
    expect(response.status()).toBe(201);

    const body = await response.json();
    expect(body.insurerId).toBeDefined();
    expect(body.insurerName).toBe(`Test Insurer ${uniqueCode}`);
    expect(body.insurerCode).toBe(uniqueCode);
    expect(body.active).toBe(true);
  });

  test('list all insurer configurations', async ({ page }) => {
    const response = await page.request.get(`${API_BASE}/insurers`);
    expect(response.ok()).toBeTruthy();

    const insurers = await response.json();
    expect(Array.isArray(insurers)).toBeTruthy();
    expect(insurers.length).toBeGreaterThanOrEqual(4); // 4 seed insurers
  });

  test('update an existing insurer configuration', async ({ page }) => {
    // First create a new insurer
    const uniqueCode = `UPD_${Date.now()}`;
    const createResponse = await page.request.post(`${API_BASE}/insurers`, {
      data: {
        insurerName: `Original Name`,
        insurerCode: uniqueCode,
        adapterType: 'MOCK',
        supportsRealTime: true,
        supportsBatch: false,
        maxBatchSize: 50,
        batchSlaHours: 24,
        rateLimitPerMinute: 30,
      },
    });
    const created = await createResponse.json();

    // Update the name
    const updateResponse = await page.request.put(`${API_BASE}/insurers/${created.insurerId}`, {
      data: {
        insurerName: 'Updated Name',
      },
    });
    expect(updateResponse.ok()).toBeTruthy();

    const updated = await updateResponse.json();
    expect(updated.insurerName).toBe('Updated Name');
  });

  test('deactivate an insurer', async ({ page }) => {
    // Create a new insurer
    const uniqueCode = `DEACT_${Date.now()}`;
    const createResponse = await page.request.post(`${API_BASE}/insurers`, {
      data: {
        insurerName: `Deactivate Test`,
        insurerCode: uniqueCode,
        adapterType: 'MOCK',
        supportsRealTime: true,
        supportsBatch: false,
        maxBatchSize: 50,
        batchSlaHours: 24,
        rateLimitPerMinute: 30,
      },
    });
    const created = await createResponse.json();

    // Deactivate
    const updateResponse = await page.request.put(`${API_BASE}/insurers/${created.insurerId}`, {
      data: {
        active: false,
      },
    });
    expect(updateResponse.ok()).toBeTruthy();

    const updated = await updateResponse.json();
    expect(updated.active).toBe(false);
  });

  test('create insurer with missing required fields returns 400', async ({ page }) => {
    const response = await page.request.post(`${API_BASE}/insurers`, {
      data: {
        insurerName: 'Incomplete Insurer',
        // missing insurerCode and other required fields
      },
    });
    expect(response.status()).toBe(400);
  });
});
