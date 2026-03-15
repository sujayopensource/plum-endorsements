import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  getReconciliationRuns,
  getReconciliationItems,
  triggerReconciliation,
} from '@/lib/reconciliation-api';
import type { ApiError } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';

export function useReconciliationRuns(insurerId: string) {
  return useQuery({
    queryKey: queryKeys.reconciliation.runsByInsurer(insurerId),
    queryFn: () => getReconciliationRuns(insurerId),
    enabled: !!insurerId,
  });
}

export function useReconciliationItems(runId: string) {
  return useQuery({
    queryKey: queryKeys.reconciliation.items(runId),
    queryFn: () => getReconciliationItems(runId),
    enabled: !!runId,
  });
}

export function useTriggerReconciliation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: triggerReconciliation,
    onSuccess: (_data, insurerId) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.reconciliation.runsByInsurer(insurerId),
      });
      toast.success('Reconciliation triggered successfully');
    },
    onError: (error: ApiError) => {
      toast.error(error.message || 'Failed to trigger reconciliation');
    },
  });
}
