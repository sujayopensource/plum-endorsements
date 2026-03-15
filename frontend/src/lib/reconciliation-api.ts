import { request } from './api-client';
import type {
  ReconciliationRun,
  ReconciliationItem,
} from '@/types/reconciliation';

export function getReconciliationRuns(
  insurerId: string,
): Promise<ReconciliationRun[]> {
  const params = new URLSearchParams({ insurerId });
  return request(`/reconciliation/runs?${params.toString()}`);
}

export function getReconciliationItems(
  runId: string,
): Promise<ReconciliationItem[]> {
  return request(`/reconciliation/runs/${runId}/items`);
}

export function triggerReconciliation(
  insurerId: string,
): Promise<ReconciliationRun> {
  const params = new URLSearchParams({ insurerId });
  return request(`/reconciliation/trigger?${params.toString()}`, {
    method: 'POST',
  });
}
