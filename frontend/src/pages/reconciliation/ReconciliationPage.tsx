import { useState } from 'react';
import { format, formatDistanceToNow } from 'date-fns';
import {
  GitCompare,
  Play,
  ChevronDown,
  ChevronRight,
  CheckCircle,
  AlertTriangle,
  XCircle,
  HelpCircle,
  Download,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { PageHeader } from '@/components/shared/PageHeader';
import { EmptyState } from '@/components/shared/EmptyState';
import { useInsurers } from '@/hooks/use-insurers';
import {
  useReconciliationRuns,
  useReconciliationItems,
  useTriggerReconciliation,
} from '@/hooks/use-reconciliation';
import { exportToCsv } from '@/lib/csv-export';

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

export function ReconciliationPage() {
  const { data: insurers } = useInsurers();
  const [selectedInsurerId, setSelectedInsurerId] = useState('');
  const { data: runs, isLoading: runsLoading } =
    useReconciliationRuns(selectedInsurerId);
  const triggerMutation = useTriggerReconciliation();
  const [expandedRunId, setExpandedRunId] = useState('');

  const selectedInsurer = insurers?.find(
    (i) => i.insurerId === selectedInsurerId,
  );
  const latestRun = runs?.[0];
  const matchCount = latestRun?.matched ?? 0;
  const partialCount = latestRun?.partialMatched ?? 0;
  const rejectedCount = latestRun?.rejected ?? 0;
  const missingCount = latestRun?.missing ?? 0;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Reconciliation"
        description="Monitor and trigger reconciliation runs across insurers"
        action={
          <div className="flex gap-2">
            {runs && runs.length > 0 && (
              <Button
                variant="outline"
                onClick={() => {
                  const headers = ['Run ID', 'Status', 'Checked', 'Matched', 'Partial', 'Rejected', 'Missing', 'Started', 'Completed'];
                  const rows = runs.map((r) => [
                    r.id, r.status, r.totalChecked, r.matched, r.partialMatched, r.rejected, r.missing,
                    r.startedAt, r.completedAt ?? '',
                  ]);
                  exportToCsv('reconciliation-runs.csv', headers, rows);
                }}
              >
                <Download className="mr-2 h-4 w-4" />
                Export CSV
              </Button>
            )}
            <Button
              disabled={!selectedInsurerId || triggerMutation.isPending}
              onClick={() => triggerMutation.mutate(selectedInsurerId)}
            >
              <Play className="mr-2 h-4 w-4" />
              {triggerMutation.isPending
                ? 'Triggering...'
                : 'Trigger Reconciliation'}
            </Button>
          </div>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Select Insurer</CardTitle>
        </CardHeader>
        <CardContent>
          <Select
            value={selectedInsurerId}
            onValueChange={(v) => {
              setSelectedInsurerId(v ?? '');
              setExpandedRunId('');
            }}
          >
            <SelectTrigger className="w-72">
              {selectedInsurer ? (
                <span className="flex flex-1 text-left">
                  {selectedInsurer.insurerName} ({selectedInsurer.insurerCode})
                </span>
              ) : (
                <SelectValue placeholder="Choose an insurer..." />
              )}
            </SelectTrigger>
            <SelectContent>
              {insurers?.map((insurer) => (
                <SelectItem key={insurer.insurerId} value={insurer.insurerId}>
                  {insurer.insurerName} ({insurer.insurerCode})
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </CardContent>
      </Card>

      {selectedInsurerId && latestRun && (
        <div className="grid gap-4 md:grid-cols-4">
          <Card>
            <CardContent className="flex items-center gap-4 pt-6">
              <div className="rounded-lg bg-green-100 p-3">
                <CheckCircle className="h-5 w-5 text-green-600" />
              </div>
              <div>
                <p className="text-muted-foreground text-sm">Matched</p>
                <p className="text-2xl font-bold">{matchCount}</p>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="flex items-center gap-4 pt-6">
              <div className="rounded-lg bg-yellow-100 p-3">
                <AlertTriangle className="h-5 w-5 text-yellow-600" />
              </div>
              <div>
                <p className="text-muted-foreground text-sm">Partial</p>
                <p className="text-2xl font-bold">{partialCount}</p>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="flex items-center gap-4 pt-6">
              <div className="rounded-lg bg-red-100 p-3">
                <XCircle className="h-5 w-5 text-red-600" />
              </div>
              <div>
                <p className="text-muted-foreground text-sm">Rejected</p>
                <p className="text-2xl font-bold">{rejectedCount}</p>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="flex items-center gap-4 pt-6">
              <div className="rounded-lg bg-purple-100 p-3">
                <HelpCircle className="h-5 w-5 text-purple-600" />
              </div>
              <div>
                <p className="text-muted-foreground text-sm">Missing</p>
                <p className="text-2xl font-bold">{missingCount}</p>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {selectedInsurerId && runsLoading && (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      )}

      {selectedInsurerId && !runsLoading && (!runs || runs.length === 0) && (
        <EmptyState
          icon={GitCompare}
          title="No reconciliation runs"
          description="No reconciliation runs have been performed for this insurer yet. Click the button above to trigger one."
        />
      )}

      {runs && runs.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Reconciliation Runs</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-10" />
                    <TableHead>Run ID</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right">Checked</TableHead>
                    <TableHead className="text-right">Matched</TableHead>
                    <TableHead className="text-right">Partial</TableHead>
                    <TableHead className="text-right">Rejected</TableHead>
                    <TableHead className="text-right">Missing</TableHead>
                    <TableHead>Started</TableHead>
                    <TableHead>Completed</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {runs.map((run) => {
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
                                run.partialMatched > 0 ? 'text-yellow-600' : ''
                              }
                            >
                              {run.partialMatched}
                            </span>
                          </TableCell>
                          <TableCell className="text-right">
                            <span
                              className={
                                run.rejected > 0 ? 'text-red-600' : ''
                              }
                            >
                              {run.rejected}
                            </span>
                          </TableCell>
                          <TableCell className="text-right">
                            <span
                              className={
                                run.missing > 0 ? 'text-purple-600' : ''
                              }
                            >
                              {run.missing}
                            </span>
                          </TableCell>
                          <TableCell
                            className="text-muted-foreground text-xs"
                            title={new Date(run.startedAt).toLocaleString()}
                          >
                            {formatDistanceToNow(new Date(run.startedAt), {
                              addSuffix: true,
                            })}
                          </TableCell>
                          <TableCell className="text-muted-foreground text-xs">
                            {run.completedAt
                              ? format(
                                  new Date(run.completedAt),
                                  'dd MMM yyyy HH:mm',
                                )
                              : '--'}
                          </TableCell>
                        </TableRow>
                        {isExpanded && (
                          <TableRow key={`${run.id}-items`}>
                            <TableCell colSpan={10} className="bg-muted/30 p-4">
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
          </CardContent>
        </Card>
      )}
    </div>
  );
}
