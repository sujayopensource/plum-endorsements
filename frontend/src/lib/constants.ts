import type { EndorsementStatus, EndorsementType } from '@/types/endorsement';

export const STATUS_CONFIG: Record<
  EndorsementStatus,
  { label: string; className: string }
> = {
  CREATED: {
    label: 'Created',
    className: 'border-gray-300 text-gray-700 bg-gray-50',
  },
  VALIDATED: {
    label: 'Validated',
    className: 'border-blue-300 text-blue-700 bg-blue-50',
  },
  PROVISIONALLY_COVERED: {
    label: 'Prov. Covered',
    className: 'border-sky-300 text-sky-800 bg-sky-100',
  },
  SUBMITTED_REALTIME: {
    label: 'Submitted (RT)',
    className: 'border-blue-300 text-blue-800 bg-blue-100',
  },
  QUEUED_FOR_BATCH: {
    label: 'Queued (Batch)',
    className: 'border-indigo-300 text-indigo-800 bg-indigo-100',
  },
  BATCH_SUBMITTED: {
    label: 'Batch Submitted',
    className: 'border-indigo-300 text-indigo-800 bg-indigo-100',
  },
  INSURER_PROCESSING: {
    label: 'Processing',
    className: 'border-yellow-300 text-yellow-800 bg-yellow-100',
  },
  CONFIRMED: {
    label: 'Confirmed',
    className: 'border-green-300 text-green-800 bg-green-100',
  },
  REJECTED: {
    label: 'Rejected',
    className: 'border-red-300 text-red-800 bg-red-100',
  },
  RETRY_PENDING: {
    label: 'Retry Pending',
    className: 'border-amber-300 text-amber-800 bg-amber-100',
  },
  FAILED_PERMANENT: {
    label: 'Failed',
    className: 'border-red-400 text-red-900 bg-red-200',
  },
};

export const TYPE_CONFIG: Record<
  EndorsementType,
  { label: string; className: string }
> = {
  ADD: {
    label: 'Add',
    className: 'border-blue-300 text-blue-800 bg-blue-100',
  },
  DELETE: {
    label: 'Delete',
    className: 'border-red-300 text-red-800 bg-red-100',
  },
  UPDATE: {
    label: 'Update',
    className: 'border-amber-300 text-amber-800 bg-amber-100',
  },
};

export const ALL_STATUSES: EndorsementStatus[] = [
  'CREATED',
  'VALIDATED',
  'PROVISIONALLY_COVERED',
  'SUBMITTED_REALTIME',
  'QUEUED_FOR_BATCH',
  'BATCH_SUBMITTED',
  'INSURER_PROCESSING',
  'CONFIRMED',
  'REJECTED',
  'RETRY_PENDING',
  'FAILED_PERMANENT',
];

export const TERMINAL_STATUSES: EndorsementStatus[] = [
  'CONFIRMED',
  'FAILED_PERMANENT',
];

export const ACTIVE_STATUSES: EndorsementStatus[] = ALL_STATUSES.filter(
  (s) => !TERMINAL_STATUSES.includes(s),
);
