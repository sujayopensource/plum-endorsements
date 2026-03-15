import { Check, Circle, X, RotateCcw } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { EndorsementStatus } from '@/types/endorsement';

const REALTIME_PATH: EndorsementStatus[] = [
  'CREATED',
  'VALIDATED',
  'PROVISIONALLY_COVERED',
  'SUBMITTED_REALTIME',
  'INSURER_PROCESSING',
  'CONFIRMED',
];

const BATCH_PATH: EndorsementStatus[] = [
  'CREATED',
  'VALIDATED',
  'PROVISIONALLY_COVERED',
  'QUEUED_FOR_BATCH',
  'BATCH_SUBMITTED',
  'INSURER_PROCESSING',
  'CONFIRMED',
];

const STEP_LABELS: Record<string, string> = {
  CREATED: 'Created',
  VALIDATED: 'Validated',
  PROVISIONALLY_COVERED: 'Prov. Covered',
  SUBMITTED_REALTIME: 'Submitted (RT)',
  QUEUED_FOR_BATCH: 'Queued',
  BATCH_SUBMITTED: 'Batch Sent',
  INSURER_PROCESSING: 'Processing',
  CONFIRMED: 'Confirmed',
  REJECTED: 'Rejected',
  RETRY_PENDING: 'Retry Pending',
  FAILED_PERMANENT: 'Failed',
};

function getPath(
  status: EndorsementStatus,
  batchId: string | null,
): EndorsementStatus[] {
  if (
    status === 'QUEUED_FOR_BATCH' ||
    status === 'BATCH_SUBMITTED' ||
    batchId
  ) {
    return BATCH_PATH;
  }
  return REALTIME_PATH;
}

function getStepState(
  step: EndorsementStatus,
  currentStatus: EndorsementStatus,
  path: EndorsementStatus[],
): 'completed' | 'current' | 'upcoming' | 'failed' {
  if (
    currentStatus === 'REJECTED' ||
    currentStatus === 'RETRY_PENDING' ||
    currentStatus === 'FAILED_PERMANENT'
  ) {
    const processingIdx = path.indexOf('INSURER_PROCESSING');
    const stepIdx = path.indexOf(step);
    if (stepIdx <= processingIdx) return 'completed';
    return 'upcoming';
  }

  const currentIdx = path.indexOf(currentStatus);
  const stepIdx = path.indexOf(step);
  if (stepIdx < currentIdx) return 'completed';
  if (stepIdx === currentIdx) return 'current';
  return 'upcoming';
}

interface StatusTimelineProps {
  status: EndorsementStatus;
  batchId: string | null;
  retryCount: number;
  failureReason: string | null;
}

export function StatusTimeline({
  status,
  batchId,
  retryCount,
  failureReason,
}: StatusTimelineProps) {
  const path = getPath(status, batchId);
  const isRejected =
    status === 'REJECTED' ||
    status === 'RETRY_PENDING' ||
    status === 'FAILED_PERMANENT';

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-1">
        {path.map((step, i) => {
          const state = getStepState(step, status, path);
          return (
            <div key={step} className="flex items-center">
              <div className="flex flex-col items-center">
                <div
                  className={cn(
                    'flex h-7 w-7 items-center justify-center rounded-full border-2 text-xs',
                    state === 'completed' &&
                      'border-green-500 bg-green-500 text-white',
                    state === 'current' &&
                      'border-blue-500 bg-blue-500 text-white',
                    state === 'upcoming' &&
                      'border-gray-300 bg-white text-gray-400',
                  )}
                >
                  {state === 'completed' ? (
                    <Check className="h-3.5 w-3.5" />
                  ) : state === 'current' ? (
                    <Circle className="h-3 w-3 fill-current" />
                  ) : (
                    <Circle className="h-3 w-3" />
                  )}
                </div>
                <span
                  className={cn(
                    'mt-1 text-[10px] leading-tight',
                    state === 'current'
                      ? 'font-semibold text-blue-600'
                      : 'text-muted-foreground',
                  )}
                >
                  {STEP_LABELS[step]}
                </span>
              </div>
              {i < path.length - 1 && (
                <div
                  className={cn(
                    'mb-4 h-0.5 w-6',
                    getStepState(path[i + 1], status, path) !== 'upcoming'
                      ? 'bg-green-500'
                      : 'bg-gray-200',
                  )}
                />
              )}
            </div>
          );
        })}
      </div>

      {isRejected && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-3">
          <div className="flex items-center gap-2">
            {status === 'FAILED_PERMANENT' ? (
              <X className="h-4 w-4 text-red-600" />
            ) : (
              <RotateCcw className="h-4 w-4 text-amber-600" />
            )}
            <span className="text-sm font-medium">
              {status === 'FAILED_PERMANENT'
                ? 'Permanently Failed'
                : status === 'RETRY_PENDING'
                  ? `Retry Pending (attempt ${retryCount}/3)`
                  : `Rejected (attempt ${retryCount}/3)`}
            </span>
          </div>
          {failureReason && (
            <p className="mt-1 text-sm text-red-700">{failureReason}</p>
          )}
        </div>
      )}
    </div>
  );
}
