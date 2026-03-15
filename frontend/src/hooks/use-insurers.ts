import { useQuery } from '@tanstack/react-query';
import {
  listInsurers,
  getInsurer,
  getInsurerCapabilities,
} from '@/lib/insurers-api';
import { queryKeys } from '@/lib/query-keys';

export function useInsurers() {
  return useQuery({
    queryKey: queryKeys.insurers.lists(),
    queryFn: () => listInsurers(),
  });
}

export function useInsurer(insurerId: string) {
  return useQuery({
    queryKey: queryKeys.insurers.detail(insurerId),
    queryFn: () => getInsurer(insurerId),
    enabled: !!insurerId,
  });
}

export function useInsurerCapabilities(insurerId: string) {
  return useQuery({
    queryKey: queryKeys.insurers.capabilities(insurerId),
    queryFn: () => getInsurerCapabilities(insurerId),
    enabled: !!insurerId,
  });
}
