export type EndorsementType = 'ADD' | 'DELETE' | 'UPDATE';

export type EndorsementStatus =
  | 'CREATED'
  | 'VALIDATED'
  | 'PROVISIONALLY_COVERED'
  | 'SUBMITTED_REALTIME'
  | 'QUEUED_FOR_BATCH'
  | 'BATCH_SUBMITTED'
  | 'INSURER_PROCESSING'
  | 'CONFIRMED'
  | 'REJECTED'
  | 'RETRY_PENDING'
  | 'FAILED_PERMANENT';

export interface EndorsementResponse {
  id: string;
  employerId: string;
  employeeId: string;
  insurerId: string;
  policyId: string;
  type: EndorsementType;
  status: EndorsementStatus;
  coverageStartDate: string;
  coverageEndDate: string | null;
  premiumAmount: number | null;
  batchId: string | null;
  insurerReference: string | null;
  retryCount: number;
  failureReason: string | null;
  idempotencyKey: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEndorsementRequest {
  employerId: string;
  employeeId: string;
  insurerId: string;
  policyId: string;
  type: string;
  coverageStartDate: string;
  coverageEndDate?: string;
  employeeData: Record<string, unknown>;
  premiumAmount?: number;
  idempotencyKey?: string;
}
