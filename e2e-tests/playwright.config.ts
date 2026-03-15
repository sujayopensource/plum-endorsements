import { defineConfig } from '@playwright/test';

// Detect if Ollama is running by checking the backend's circuit breaker registry.
// When Ollama is enabled, anomaly detection uses LLM which adds ~30s latency.
const OLLAMA_ENABLED = process.env.OLLAMA_ENABLED === 'true';
const DEFAULT_TIMEOUT = 30_000;
const OLLAMA_TIMEOUT = 120_000;
const testTimeout = OLLAMA_ENABLED ? OLLAMA_TIMEOUT : DEFAULT_TIMEOUT;
const expectTimeout = OLLAMA_ENABLED ? 30_000 : 10_000;

export default defineConfig({
  testDir: './tests',
  timeout: testTimeout,
  expect: {
    timeout: expectTimeout,
  },
  fullyParallel: false,
  retries: 1,
  reporter: [
    ['list'],
    ['allure-playwright', { outputFolder: 'allure-results' }],
  ],
  projects: [
    {
      name: 'storybook',
      testDir: './tests/storybook',
      use: {
        baseURL: 'http://localhost:6006',
      },
    },
    {
      name: 'e2e',
      testDir: './tests/e2e',
      use: {
        baseURL: 'http://localhost:5173',
      },
    },
  ],
  use: {
    headless: true,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
});
