import { useParams, Link } from 'react-router-dom';
import { format } from 'date-fns';
import { ArrowLeft, Copy, Check } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Separator } from '@/components/ui/separator';
import { StatusBadge } from '@/components/shared/StatusBadge';
import { TypeBadge } from '@/components/shared/TypeBadge';
import { StatusTimeline } from './components/StatusTimeline';
import { EndorsementActions } from './components/EndorsementActions';
import { useEndorsement } from '@/hooks/use-endorsements';
import { useProvisionalCoverage } from '@/hooks/use-coverage';

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);
  return (
    <button
      onClick={() => {
        navigator.clipboard.writeText(text);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      }}
      className="text-muted-foreground hover:text-foreground ml-1 inline-flex"
      title="Copy to clipboard"
      aria-label="Copy endorsement ID"
    >
      {copied ? (
        <Check className="h-3 w-3 text-green-500" />
      ) : (
        <Copy className="h-3 w-3" />
      )}
    </button>
  );
}

function DetailRow({
  label,
  value,
  mono,
}: {
  label: string;
  value: React.ReactNode;
  mono?: boolean;
}) {
  return (
    <div className="flex items-start justify-between py-2">
      <span className="text-muted-foreground text-sm">{label}</span>
      <span className={`text-right text-sm ${mono ? 'font-mono' : ''}`}>
        {value}
      </span>
    </div>
  );
}

export function EndorsementDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: endorsement, isLoading } = useEndorsement(id!);
  const { data: coverage } = useProvisionalCoverage(id!);

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <div className="grid gap-6 md:grid-cols-3">
          <div className="space-y-4 md:col-span-2">
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-64 w-full" />
          </div>
          <div className="space-y-4">
            <Skeleton className="h-48 w-full" />
            <Skeleton className="h-32 w-full" />
          </div>
        </div>
      </div>
    );
  }

  if (!endorsement) return null;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" asChild>
          <Link to="/endorsements">
            <ArrowLeft className="h-4 w-4" />
          </Link>
        </Button>
        <div>
          <h1 className="text-2xl font-bold tracking-tight">
            Endorsement Detail
          </h1>
          <p className="text-muted-foreground font-mono text-sm">
            {endorsement.id}
          </p>
        </div>
      </div>

      <StatusTimeline
        status={endorsement.status}
        batchId={endorsement.batchId}
        retryCount={endorsement.retryCount}
        failureReason={endorsement.failureReason}
      />

      <div className="grid gap-6 md:grid-cols-3">
        <div className="space-y-6 md:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-0">
              <DetailRow
                label="Endorsement ID"
                value={
                  <span className="flex items-center">
                    {endorsement.id.substring(0, 16)}...
                    <CopyButton text={endorsement.id} />
                  </span>
                }
                mono
              />
              <Separator />
              <DetailRow
                label="Type"
                value={<TypeBadge type={endorsement.type} />}
              />
              <Separator />
              <DetailRow
                label="Status"
                value={<StatusBadge status={endorsement.status} />}
              />
              <Separator />
              <DetailRow
                label="Employer ID"
                value={
                  <span className="flex items-center">
                    {endorsement.employerId.substring(0, 8)}...
                    <CopyButton text={endorsement.employerId} />
                  </span>
                }
                mono
              />
              <Separator />
              <DetailRow
                label="Employee ID"
                value={
                  <span className="flex items-center">
                    {endorsement.employeeId.substring(0, 8)}...
                    <CopyButton text={endorsement.employeeId} />
                  </span>
                }
                mono
              />
              <Separator />
              <DetailRow
                label="Insurer ID"
                value={endorsement.insurerId.substring(0, 8) + '...'}
                mono
              />
              <Separator />
              <DetailRow
                label="Policy ID"
                value={endorsement.policyId.substring(0, 8) + '...'}
                mono
              />
              <Separator />
              <DetailRow
                label="Coverage Period"
                value={`${format(new Date(endorsement.coverageStartDate), 'dd MMM yyyy')}${
                  endorsement.coverageEndDate
                    ? ` - ${format(new Date(endorsement.coverageEndDate), 'dd MMM yyyy')}`
                    : ''
                }`}
              />
              <Separator />
              <DetailRow
                label="Premium Amount"
                value={
                  endorsement.premiumAmount != null
                    ? `₹${endorsement.premiumAmount.toLocaleString('en-IN')}`
                    : '--'
                }
              />
              {endorsement.insurerReference && (
                <>
                  <Separator />
                  <DetailRow
                    label="Insurer Reference"
                    value={endorsement.insurerReference}
                  />
                </>
              )}
              {endorsement.batchId && (
                <>
                  <Separator />
                  <DetailRow
                    label="Batch ID"
                    value={endorsement.batchId.substring(0, 8) + '...'}
                    mono
                  />
                </>
              )}
              <Separator />
              <DetailRow
                label="Retry Count"
                value={
                  <span
                    className={
                      endorsement.retryCount > 0 ? 'text-amber-600' : ''
                    }
                  >
                    {endorsement.retryCount} / 3
                  </span>
                }
              />
              <Separator />
              <DetailRow
                label="Idempotency Key"
                value={
                  endorsement.idempotencyKey.length > 30
                    ? endorsement.idempotencyKey.substring(0, 30) + '...'
                    : endorsement.idempotencyKey
                }
              />
              <Separator />
              <DetailRow
                label="Created"
                value={format(
                  new Date(endorsement.createdAt),
                  'dd MMM yyyy HH:mm:ss',
                )}
              />
              <Separator />
              <DetailRow
                label="Updated"
                value={format(
                  new Date(endorsement.updatedAt),
                  'dd MMM yyyy HH:mm:ss',
                )}
              />
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <EndorsementActions
            id={endorsement.id}
            status={endorsement.status}
            retryCount={endorsement.retryCount}
          />

          {coverage && (
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Coverage</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Type</span>
                  <span
                    className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                      coverage.coverageType === 'CONFIRMED'
                        ? 'bg-green-100 text-green-800'
                        : 'bg-yellow-100 text-yellow-800'
                    }`}
                  >
                    {coverage.coverageType}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Start</span>
                  <span>
                    {format(new Date(coverage.coverageStart), 'dd MMM yyyy')}
                  </span>
                </div>
                {coverage.confirmedAt && (
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Confirmed</span>
                    <span>
                      {format(
                        new Date(coverage.confirmedAt),
                        'dd MMM yyyy HH:mm',
                      )}
                    </span>
                  </div>
                )}
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Active</span>
                  <span>{coverage.active ? 'Yes' : 'No'}</span>
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
