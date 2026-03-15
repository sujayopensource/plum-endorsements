import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { format, formatDistanceToNow } from 'date-fns';
import { ArrowLeft, Play, ChevronDown, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Separator } from '@/components/ui/separator';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useInsurer, useInsurerCapabilities } from '@/hooks/use-insurers';
import {
  useReconciliationRuns,
  useReconciliationItems,
  useTriggerReconciliation,
} from '@/hooks/use-reconciliation';

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

function RunItemsSection({ runId }: { runId: string }) {
  const { data: items, isLoading } = useReconciliationItems(runId);

  if (isLoading) {
    return <Skeleton className="h-24 w-full" />;
  }

  if (!items || items.length === 0) {
    return (
      <p className="text-muted-foreground py-2 text-center text-sm">
        No items found for this run
      </p>
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Endorsement</TableHead>
          <TableHead>Batch</TableHead>
          <TableHead>Employer</TableHead>
          <TableHead>Outcome</TableHead>
          <TableHead>Action Taken</TableHead>
          <TableHead>Created</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {items.map((item) => (
          <TableRow key={item.id}>
            <TableCell className="font-mono text-xs">
              {item.endorsementId.substring(0, 8)}...
            </TableCell>
            <TableCell className="font-mono text-xs">
              {item.batchId ? `${item.batchId.substring(0, 8)}...` : '--'}
            </TableCell>
            <TableCell className="font-mono text-xs">
              {item.employerId.substring(0, 8)}...
            </TableCell>
            <TableCell>
              <Badge
                variant={
                  item.outcome === 'MATCH'
                    ? 'default'
                    : item.outcome === 'PARTIAL_MATCH'
                      ? 'secondary'
                      : 'destructive'
                }
              >
                {item.outcome}
              </Badge>
            </TableCell>
            <TableCell className="text-xs">
              {item.actionTaken || '--'}
            </TableCell>
            <TableCell className="text-muted-foreground text-xs">
              {format(new Date(item.createdAt), 'dd MMM yyyy HH:mm')}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

export function InsurerDetailPage() {
  const { insurerId } = useParams<{ insurerId: string }>();
  const { data: insurer, isLoading } = useInsurer(insurerId!);
  const { data: capabilities } = useInsurerCapabilities(insurerId!);
  const { data: runs } = useReconciliationRuns(insurerId!);
  const triggerMutation = useTriggerReconciliation();
  const [expandedRunId, setExpandedRunId] = useState('');

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <div className="grid gap-6 md:grid-cols-3">
          <div className="space-y-4 md:col-span-2">
            <Skeleton className="h-64 w-full" />
          </div>
          <div className="space-y-4">
            <Skeleton className="h-48 w-full" />
          </div>
        </div>
      </div>
    );
  }

  if (!insurer) return null;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link to="/insurers" className="inline-flex items-center justify-center rounded-md p-2 hover:bg-accent">
          <ArrowLeft className="h-4 w-4" />
        </Link>
        <div>
          <h1 className="text-2xl font-bold tracking-tight">
            {insurer.insurerName}
          </h1>
          <p className="text-muted-foreground font-mono text-sm">
            {insurer.insurerId}
          </p>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-3">
        <div className="space-y-6 md:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Configuration</CardTitle>
            </CardHeader>
            <CardContent className="space-y-0">
              <DetailRow label="Insurer ID" value={insurer.insurerId} mono />
              <Separator />
              <DetailRow label="Name" value={insurer.insurerName} />
              <Separator />
              <DetailRow label="Code" value={insurer.insurerCode} mono />
              <Separator />
              <DetailRow label="Adapter Type" value={insurer.adapterType} />
              <Separator />
              <DetailRow label="Data Format" value={insurer.dataFormat} />
              <Separator />
              <DetailRow
                label="Rate Limit"
                value={`${insurer.rateLimitPerMinute} / min`}
              />
              <Separator />
              <DetailRow
                label="Status"
                value={
                  <Badge variant={insurer.active ? 'default' : 'secondary'}>
                    {insurer.active ? 'Active' : 'Inactive'}
                  </Badge>
                }
              />
              <Separator />
              <DetailRow
                label="Created"
                value={format(
                  new Date(insurer.createdAt),
                  'dd MMM yyyy HH:mm:ss',
                )}
              />
              <Separator />
              <DetailRow
                label="Updated"
                value={format(
                  new Date(insurer.updatedAt),
                  'dd MMM yyyy HH:mm:ss',
                )}
              />
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-3">
              <CardTitle className="text-base">Reconciliation Runs</CardTitle>
              <Button
                size="sm"
                disabled={triggerMutation.isPending}
                onClick={() => triggerMutation.mutate(insurerId!)}
              >
                <Play className="mr-2 h-4 w-4" />
                {triggerMutation.isPending ? 'Triggering...' : 'Trigger Reconciliation'}
              </Button>
            </CardHeader>
            <CardContent>
              {!runs || runs.length === 0 ? (
                <p className="text-muted-foreground py-4 text-center text-sm">
                  No reconciliation runs yet
                </p>
              ) : (
                <div className="space-y-2">
                  <div className="rounded-md border">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead className="w-10" />
                          <TableHead>Run ID</TableHead>
                          <TableHead>Status</TableHead>
                          <TableHead className="text-right">Checked</TableHead>
                          <TableHead className="text-right">Matched</TableHead>
                          <TableHead className="text-right">Discrepancies</TableHead>
                          <TableHead>Started</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {runs.map((run) => {
                          const discrepancies =
                            run.partialMatched + run.rejected + run.missing;
                          const isExpanded = expandedRunId === run.id;
                          return (
                            <>
                              <TableRow
                                key={run.id}
                                className="cursor-pointer"
                                onClick={() =>
                                  setExpandedRunId(isExpanded ? '' : run.id)
                                }
                              >
                                <TableCell>
                                  {isExpanded ? (
                                    <ChevronDown className="h-4 w-4" />
                                  ) : (
                                    <ChevronRight className="h-4 w-4" />
                                  )}
                                </TableCell>
                                <TableCell className="font-mono text-xs">
                                  {run.id.substring(0, 8)}...
                                </TableCell>
                                <TableCell>
                                  <Badge
                                    variant={
                                      run.status === 'COMPLETED'
                                        ? 'default'
                                        : run.status === 'RUNNING'
                                          ? 'secondary'
                                          : 'outline'
                                    }
                                  >
                                    {run.status}
                                  </Badge>
                                </TableCell>
                                <TableCell className="text-right">
                                  {run.totalChecked}
                                </TableCell>
                                <TableCell className="text-right">
                                  {run.matched}
                                </TableCell>
                                <TableCell className="text-right">
                                  <span
                                    className={
                                      discrepancies > 0
                                        ? 'text-amber-600'
                                        : ''
                                    }
                                  >
                                    {discrepancies}
                                  </span>
                                </TableCell>
                                <TableCell
                                  className="text-muted-foreground text-xs"
                                  title={new Date(
                                    run.startedAt,
                                  ).toLocaleString()}
                                >
                                  {formatDistanceToNow(
                                    new Date(run.startedAt),
                                    { addSuffix: true },
                                  )}
                                </TableCell>
                              </TableRow>
                              {isExpanded && (
                                <TableRow key={`${run.id}-items`}>
                                  <TableCell colSpan={7} className="bg-muted/30 p-4">
                                    <RunItemsSection runId={run.id} />
                                  </TableCell>
                                </TableRow>
                              )}
                            </>
                          );
                        })}
                      </TableBody>
                    </Table>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          {capabilities && (
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Capabilities</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Real-time</span>
                  <Badge
                    variant={
                      capabilities.supportsRealTime ? 'default' : 'outline'
                    }
                  >
                    {capabilities.supportsRealTime ? 'Supported' : 'Not Supported'}
                  </Badge>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Batch</span>
                  <Badge
                    variant={
                      capabilities.supportsBatch ? 'default' : 'outline'
                    }
                  >
                    {capabilities.supportsBatch ? 'Supported' : 'Not Supported'}
                  </Badge>
                </div>
                <Separator />
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Max Batch Size</span>
                  <span>{capabilities.maxBatchSize}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Batch SLA</span>
                  <span>{capabilities.batchSlaHours}h</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Rate Limit</span>
                  <span>{capabilities.rateLimitPerMinute}/min</span>
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
