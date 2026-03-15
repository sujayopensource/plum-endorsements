import {
  useQuery,
  useMutation,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  listEndorsements,
  getEndorsement,
  createEndorsement,
  submitToInsurer,
  confirmEndorsement,
  rejectEndorsement,
  getBatchProgress,
  getOutstandingItems,
  type ListEndorsementsParams,
} from '@/lib/endorsements-api';
import type { ApiError } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';
import type { EndorsementResponse } from '@/types/endorsement';

export function useEndorsements(params: ListEndorsementsParams) {
  return useQuery({
    queryKey: queryKeys.endorsements.list(params as unknown as Record<string, unknown>),
    queryFn: () => listEndorsements(params),
    placeholderData: keepPreviousData,
    enabled: !!params.employerId,
  });
}

export function useEndorsement(id: string) {
  return useQuery({
    queryKey: queryKeys.endorsements.detail(id),
    queryFn: () => getEndorsement(id),
    enabled: !!id,
  });
}

export function useCreateEndorsement() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createEndorsement,
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.endorsements.lists(),
      });
      toast.success('Endorsement created successfully');
    },
    onError: (error: ApiError) => {
      toast.error(error.message || 'Failed to create endorsement');
    },
  });
}

export function useSubmitEndorsement() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: submitToInsurer,
    onSuccess: (_data, id) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.endorsements.detail(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.endorsements.lists(),
      });
      toast.success('Endorsement submitted to insurer');
    },
    onError: (error: ApiError) => {
      toast.error(error.message || 'Failed to submit endorsement');
    },
  });
}

export function useConfirmEndorsement() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, insurerReference }: { id: string; insurerReference: string }) =>
      confirmEndorsement(id, insurerReference),
    onMutate: async ({ id }) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.endorsements.detail(id) });
      const previous = queryClient.getQueryData<EndorsementResponse>(
        queryKeys.endorsements.detail(id),
      );
      queryClient.setQueryData<EndorsementResponse>(
        queryKeys.endorsements.detail(id),
        (old) => (old ? { ...old, status: 'CONFIRMED' as const } : old),
      );
      return { previous };
    },
    onSuccess: (_data, { id }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.endorsements.detail(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.endorsements.lists(),
      });
      toast.success('Endorsement confirmed');
    },
    onError: (error: ApiError, { id }, context) => {
      if (context?.previous) {
        queryClient.setQueryData(queryKeys.endorsements.detail(id), context.previous);
      }
      toast.error(error.message || 'Failed to confirm endorsement');
    },
  });
}

export function useRejectEndorsement() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      rejectEndorsement(id, reason),
    onMutate: async ({ id }) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.endorsements.detail(id) });
      const previous = queryClient.getQueryData<EndorsementResponse>(
        queryKeys.endorsements.detail(id),
      );
      queryClient.setQueryData<EndorsementResponse>(
        queryKeys.endorsements.detail(id),
        (old) => (old ? { ...old, status: 'REJECTED' as const } : old),
      );
      return { previous };
    },
    onSuccess: (_data, { id }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.endorsements.detail(id),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.endorsements.lists(),
      });
      toast.success('Endorsement rejected');
    },
    onError: (error: ApiError, { id }, context) => {
      if (context?.previous) {
        queryClient.setQueryData(queryKeys.endorsements.detail(id), context.previous);
      }
      toast.error(error.message || 'Failed to reject endorsement');
    },
  });
}

// --- Batch Progress ---

export function useBatchProgress(employerId: string, page: number, size: number) {
  return useQuery({
    queryKey: queryKeys.endorsements.batchList({ employerId, page, size }),
    queryFn: () => getBatchProgress(employerId, page, size),
    placeholderData: keepPreviousData,
    enabled: !!employerId,
  });
}

// --- Outstanding Items ---

export function useOutstandingItems(employerId: string, page: number, size: number) {
  return useQuery({
    queryKey: queryKeys.endorsements.outstandingList({ employerId, page, size }),
    queryFn: () => getOutstandingItems(employerId, page, size),
    placeholderData: keepPreviousData,
    enabled: !!employerId,
  });
}
