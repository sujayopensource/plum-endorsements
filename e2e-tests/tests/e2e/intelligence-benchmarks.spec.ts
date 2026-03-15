import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080/api/v1';

test.describe('Intelligence - Cross-Insurer Benchmarking @allure.label.epic:Intelligence_E2E', () => {
  test('benchmarks API returns list of insurer benchmarks', async ({ page }) => {
    const response = await page.request.get(`${API_BASE}/intelligence/benchmarks`);
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(Array.isArray(body)).toBeTruthy();
  });

  test('each benchmark has required fields', async ({ page }) => {
    const response = await page.request.get(`${API_BASE}/intelligence/benchmarks`);
    const benchmarks = await response.json();

    if (benchmarks.length > 0) {
      const benchmark = benchmarks[0];
      expect(benchmark.insurerName).toBeDefined();
      expect(benchmark.insurerCode).toBeDefined();
      expect(benchmark.avgProcessingMs).toBeDefined();
      expect(benchmark.stpRate).toBeDefined();
    }
  });

  test('benchmarks are sorted by STP rate descending', async ({ page }) => {
    const response = await page.request.get(`${API_BASE}/intelligence/benchmarks`);
    const benchmarks = await response.json();

    for (let i = 0; i < benchmarks.length - 1; i++) {
      expect(benchmarks[i].stpRate).toBeGreaterThanOrEqual(benchmarks[i + 1].stpRate);
    }
  });

  test('benchmark processing times are non-negative', async ({ page }) => {
    const response = await page.request.get(`${API_BASE}/intelligence/benchmarks`);
    const benchmarks = await response.json();

    for (const benchmark of benchmarks) {
      expect(benchmark.avgProcessingMs).toBeGreaterThanOrEqual(0);
      if (benchmark.p95ProcessingMs !== undefined) {
        expect(benchmark.p95ProcessingMs).toBeGreaterThanOrEqual(benchmark.avgProcessingMs);
      }
      if (benchmark.p99ProcessingMs !== undefined) {
        expect(benchmark.p99ProcessingMs).toBeGreaterThanOrEqual(benchmark.avgProcessingMs);
      }
    }
  });
});
