export interface ReconciliationRun {
  id: string;
  insurerId: string;
  status: string;
  totalChecked: number;
  matched: number;
  partialMatched: number;
  rejected: number;
  missing: number;
  startedAt: string;
  completedAt: string | null;
}

export interface ReconciliationItem {
  id: string;
  runId: string;
  endorsementId: string;
  batchId: string | null;
  insurerId: string;
  employerId: string;
  outcome: 'MATCH' | 'PARTIAL_MATCH' | 'REJECTED' | 'MISSING';
  actionTaken: string | null;
  createdAt: string;
}
