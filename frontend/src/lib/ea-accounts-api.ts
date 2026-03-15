import { request } from './api-client';
import type { EAAccountResponse } from '@/types/ea-account';

export function getEAAccount(
  employerId: string,
  insurerId: string,
): Promise<EAAccountResponse | null> {
  const params = new URLSearchParams({ employerId, insurerId });
  return request<EAAccountResponse>(
    `/ea-accounts?${params.toString()}`,
  ).catch((err) => {
    if (err.status === 404) return null;
    throw err;
  });
}
