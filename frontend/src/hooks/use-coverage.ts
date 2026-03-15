import { useQuery } from '@tanstack/react-query';
import { getProvisionalCoverage } from '@/lib/endorsements-api';
import { queryKeys } from '@/lib/query-keys';

export function useProvisionalCoverage(endorsementId: string) {
  return useQuery({
    queryKey: queryKeys.endorsements.coverage(endorsementId),
    queryFn: () => getProvisionalCoverage(endorsementId),
    enabled: !!endorsementId,
    retry: false,
  });
}
