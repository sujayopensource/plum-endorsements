import { useQuery } from '@tanstack/react-query';
import { getEAAccount } from '@/lib/ea-accounts-api';
import { queryKeys } from '@/lib/query-keys';

export function useEAAccount(employerId: string, insurerId: string) {
  return useQuery({
    queryKey: queryKeys.eaAccounts.detail(employerId, insurerId),
    queryFn: () => getEAAccount(employerId, insurerId),
    enabled: !!employerId && !!insurerId,
    retry: false,
  });
}
