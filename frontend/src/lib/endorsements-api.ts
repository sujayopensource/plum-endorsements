import { request } from './api-client';
import type {
  EndorsementResponse,
  CreateEndorsementRequest,
  EndorsementStatus,
} from '@/types/endorsement';
import type { Page } from '@/types/api';
import type { ProvisionalCoverage } from '@/types/provisional-coverage';

export function createEndorsement(
  data: CreateEndorsementRequest,
): Promise<EndorsementResponse> {
  return request('/endorsements', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function getEndorsement(id: string): Promise<EndorsementResponse> {
  return request(`/endorsements/${id}`);
}

export interface ListEndorsementsParams {
  employerId: string;
  statuses?: EndorsementStatus[];
  page?: number;
  size?: number;
}

export function listEndorsements(
  params: ListEndorsementsParams,
): Promise<Page<EndorsementResponse>> {
  const searchParams = new URLSearchParams();
  searchParams.set('employerId', params.employerId);
  if (params.page !== undefined) searchParams.set('page', String(params.page));
  if (params.size !== undefined) searchParams.set('size', String(params.size));
  if (params.statuses) {
    params.statuses.forEach((s) => searchParams.append('statuses', s));
  }
  return request(`/endorsements?${searchParams.toString()}`);
}

export function submitToInsurer(id: string): Promise<void> {
  return request(`/endorsements/${id}/submit`, { method: 'POST' });
}

export function confirmEndorsement(
  id: string,
  insurerReference: string,
): Promise<void> {
  return request(
    `/endorsements/${id}/confirm?insurerReference=${encodeURIComponent(insurerReference)}`,
    { method: 'POST' },
  );
}

export function rejectEndorsement(
  id: string,
  reason: string,
): Promise<void> {
  return request(
    `/endorsements/${id}/reject?reason=${encodeURIComponent(reason)}`,
    { method: 'POST' },
  );
}

export function getProvisionalCoverage(
  id: string,
): Promise<ProvisionalCoverage | null> {
  return request<ProvisionalCoverage>(`/endorsements/${id}/coverage`).catch(
    (err) => {
      if (err.status === 404) return null;
      throw err;
    },
  );
}

// --- Batch Progress ---

export interface BatchProgressItem {
  id: string;
  insurerId: string;
  status: string;
  endorsementCount: number;
  submittedAt: string | null;
  completedAt: string | null;
  insurerReference: string | null;
}

export function getBatchProgress(
  employerId: string,
  page: number,
  size: number,
): Promise<Page<BatchProgressItem>> {
  return request(
    `/endorsements/employers/${employerId}/batches?page=${page}&size=${size}`,
  );
}

// --- Outstanding Items ---

export interface OutstandingItem {
  id: string;
  endorsementId: string;
  employerId: string;
  type: string;
  description: string;
  createdAt: string;
}

export function getOutstandingItems(
  employerId: string,
  page: number,
  size: number,
): Promise<Page<OutstandingItem>> {
  return request(
    `/endorsements/employers/${employerId}/outstanding?page=${page}&size=${size}`,
  );
}
