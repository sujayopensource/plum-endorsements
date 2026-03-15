import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  fetchAnomalies,
  reviewAnomaly,
  fetchForecast,
  fetchForecastHistory,
  generateForecast,
  fetchErrorResolutions,
  fetchErrorResolutionStats,
  approveResolution,
  fetchProcessMiningMetrics,
  fetchProcessMiningInsights,
  fetchStpRate,
  triggerProcessMiningAnalysis,
} from '@/lib/intelligence-api';
import type { ApiError } from '@/lib/api-client';

const INTELLIGENCE_KEYS = {
  anomalies: (params?: { employerId?: string; status?: string }) =>
    ['intelligence', 'anomalies', params] as const,
  forecast: (employerId: string, insurerId: string) =>
    ['intelligence', 'forecast', employerId, insurerId] as const,
  forecastHistory: (employerId: string) =>
    ['intelligence', 'forecast-history', employerId] as const,
  errorResolutions: (endorsementId?: string) =>
    ['intelligence', 'error-resolutions', endorsementId] as const,
  errorResolutionStats: () => ['intelligence', 'error-resolution-stats'] as const,
  processMiningMetrics: (insurerId?: string) =>
    ['intelligence', 'process-mining-metrics', insurerId] as const,
  processMiningInsights: () => ['intelligence', 'process-mining-insights'] as const,
  stpRate: (insurerId?: string) => ['intelligence', 'stp-rate', insurerId] as const,
};

export function useAnomalies(params?: { employerId?: string; status?: string }) {
  return useQuery({
    queryKey: INTELLIGENCE_KEYS.anomalies(params),
    queryFn: () => fetchAnomalies(params),
  });
}

export function useReviewAnomaly() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status, notes }: { id: string; status: string; notes?: string }) =>
      reviewAnomaly(id, status, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['intelligence', 'anomalies'] });
      toast.success('Anomaly review updated');
    },
    onError: (error: ApiError) => {
      toast.error(error.message || 'Failed to review anomaly');
    },
  });
}

export function useForecast(employerId: string, insurerId: string) {
  return useQuery({
    queryKey: INTELLIGENCE_KEYS.forecast(employerId, insurerId),
    queryFn: () => fetchForecast(employerId, insurerId),
    enabled: !!employerId && !!insurerId,
  });
}

export function useForecastHistory(employerId: string) {
  return useQuery({
    queryKey: INTELLIGENCE_KEYS.forecastHistory(employerId),
    queryFn: () => fetchForecastHistory(employerId),
    enabled: !!employerId,
  });
}

export function useGenerateForecast() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ employerId, insurerId }: { employerId: string; insurerId: string }) =>
      generateForecast(employerId, insurerId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['intelligence', 'forecast'] });
      toast.success('Forecast generated successfully');
    },
    onError: (error: ApiError) => {
      toast.error(error.message || 'Failed to generate forecast');
    },
  });
}

export function useErrorResolutions(endorsementId?: string) {
  return useQuery({
    queryKey: INTELLIGENCE_KEYS.errorResolutions(endorsementId),
    queryFn: () => fetchErrorResolutions(endorsementId),
  });
}

export function useErrorResolutionStats() {
  return useQuery({
    queryKey: INTELLIGENCE_KEYS.errorResolutionStats(),
    queryFn: fetchErrorResolutionStats,
  });
}

export function useApproveResolution() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => approveResolution(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['intelligence', 'error-resolutions'] });
      queryClient.invalidateQueries({ queryKey: ['intelligence', 'error-resolution-stats'] });
      toast.success('Resolution approved');
    },
    onError: (error: ApiError) => {
      toast.error(error.message || 'Failed to approve resolution');
    },
  });
}

export function useProcessMiningMetrics(insurerId?: string) {
  return useQuery({
    queryKey: INTELLIGENCE_KEYS.processMiningMetrics(insurerId),
    queryFn: () => fetchProcessMiningMetrics(insurerId),
  });
}

export function useProcessMiningInsights() {
  return useQuery({
    queryKey: INTELLIGENCE_KEYS.processMiningInsights(),
    queryFn: fetchProcessMiningInsights,
  });
}

export function useStpRate(insurerId?: string) {
  return useQuery({
    queryKey: INTELLIGENCE_KEYS.stpRate(insurerId),
    queryFn: () => fetchStpRate(insurerId),
  });
}

export function useTriggerAnalysis() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: triggerProcessMiningAnalysis,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['intelligence'] });
      toast.success('Analysis triggered');
    },
    onError: (error: ApiError) => {
      toast.error(error.message || 'Failed to trigger analysis');
    },
  });
}
