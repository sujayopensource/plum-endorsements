import { request } from './api-client';
import type {
  AnomalyDetection,
  BalanceForecast,
  ErrorResolution,
  ErrorResolutionStats,
  ProcessMiningMetric,
  ProcessMiningInsight,
  StpRate,
} from '@/types/intelligence';

// --- Anomaly Detection ---

export function fetchAnomalies(params?: { employerId?: string; status?: string }) {
  const searchParams = new URLSearchParams();
  if (params?.employerId) searchParams.set('employerId', params.employerId);
  if (params?.status) searchParams.set('status', params.status);
  const query = searchParams.toString();
  return request<AnomalyDetection[]>(`/intelligence/anomalies${query ? `?${query}` : ''}`);
}

export function reviewAnomaly(id: string, status: string, notes?: string) {
  return request<AnomalyDetection>(`/intelligence/anomalies/${id}/review`, {
    method: 'PUT',
    body: JSON.stringify({ status, notes }),
  });
}

// --- Balance Forecasting ---

export function fetchForecast(employerId: string, insurerId: string) {
  return request<BalanceForecast>(
    `/intelligence/forecasts?employerId=${employerId}&insurerId=${insurerId}`,
  );
}

export function fetchForecastHistory(employerId: string) {
  return request<BalanceForecast[]>(`/intelligence/forecasts/history?employerId=${employerId}`);
}

export function generateForecast(employerId: string, insurerId: string) {
  return request<BalanceForecast>(
    `/intelligence/forecasts/generate?employerId=${employerId}&insurerId=${insurerId}`,
    { method: 'POST' },
  );
}

// --- Error Resolution ---

export function fetchErrorResolutions(endorsementId?: string) {
  const query = endorsementId ? `?endorsementId=${endorsementId}` : '';
  return request<ErrorResolution[]>(`/intelligence/error-resolutions${query}`);
}

export function fetchErrorResolutionStats() {
  return request<ErrorResolutionStats>('/intelligence/error-resolutions/stats');
}

export function approveResolution(id: string) {
  return request<void>(`/intelligence/error-resolutions/${id}/approve`, { method: 'POST' });
}

// --- Process Mining ---

export function fetchProcessMiningMetrics(insurerId?: string) {
  const query = insurerId ? `?insurerId=${insurerId}` : '';
  return request<ProcessMiningMetric[]>(`/intelligence/process-mining/metrics${query}`);
}

export function fetchProcessMiningInsights() {
  return request<ProcessMiningInsight[]>('/intelligence/process-mining/insights');
}

export function fetchStpRate(insurerId?: string) {
  const query = insurerId ? `?insurerId=${insurerId}` : '';
  return request<StpRate>(`/intelligence/process-mining/stp-rate${query}`);
}

export function triggerProcessMiningAnalysis() {
  return request<void>('/intelligence/process-mining/analyze', { method: 'POST' });
}
