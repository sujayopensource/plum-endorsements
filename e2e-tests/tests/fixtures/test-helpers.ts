import { type Page, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080/api/v1';

export const TEST_IDS = {
  employerId: '11111111-1111-1111-1111-111111111111',
  insurerId: '22222222-2222-2222-2222-222222222222',
  policyId: '33333333-3333-3333-3333-333333333333',
  employeeId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
};

export const INSURER_IDS = {
  mock: '22222222-2222-2222-2222-222222222222',
  icici: '33333333-3333-3333-3333-333333333333',
  nivaBupa: '44444444-4444-4444-4444-444444444444',
  bajaj: '55555555-5555-5555-5555-555555555555',
};

export async function createEndorsementViaApi(
  overrides: Record<string, unknown> = {},
): Promise<Record<string, unknown>> {
  const body = {
    employerId: TEST_IDS.employerId,
    employeeId: overrides.employeeId ?? TEST_IDS.employeeId,
    insurerId: TEST_IDS.insurerId,
    policyId: TEST_IDS.policyId,
    type: overrides.type ?? 'ADD',
    coverageStartDate: overrides.coverageStartDate ?? new Date().toISOString().split('T')[0],
    employeeData: overrides.employeeData ?? { name: 'Test Employee' },
    premiumAmount: overrides.premiumAmount ?? 1500.0,
    ...overrides,
  };

  const response = await fetch(`${API_BASE}/endorsements`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    throw new Error(`Failed to create endorsement: ${response.status} ${await response.text()}`);
  }

  return response.json();
}

export async function submitEndorsementViaApi(id: string): Promise<void> {
  const response = await fetch(`${API_BASE}/endorsements/${id}/submit`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });

  if (!response.ok && response.status !== 202) {
    throw new Error(`Failed to submit endorsement: ${response.status}`);
  }
}

export async function confirmEndorsementViaApi(
  id: string,
  insurerReference: string,
): Promise<void> {
  const response = await fetch(
    `${API_BASE}/endorsements/${id}/confirm?insurerReference=${encodeURIComponent(insurerReference)}`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    },
  );

  if (!response.ok && response.status !== 202) {
    throw new Error(`Failed to confirm endorsement: ${response.status}`);
  }
}

export async function rejectEndorsementViaApi(
  id: string,
  reason: string,
): Promise<void> {
  const response = await fetch(
    `${API_BASE}/endorsements/${id}/reject?reason=${encodeURIComponent(reason)}`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    },
  );

  if (!response.ok && response.status !== 202) {
    throw new Error(`Failed to reject endorsement: ${response.status}`);
  }
}

export async function getEndorsementViaApi(id: string): Promise<Record<string, unknown>> {
  const response = await fetch(`${API_BASE}/endorsements/${id}`);
  return response.json();
}

export function generateEmployeeId(): string {
  return crypto.randomUUID();
}

export async function waitForText(page: Page, text: string, timeout = 10_000): Promise<void> {
  await expect(page.getByText(text).first()).toBeVisible({ timeout });
}
