import { useState } from 'react';
import { format, formatDistanceToNow } from 'date-fns';
import { Layers } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
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
import { Pagination } from '@/components/shared/Pagination';
import { useBatchProgress } from '@/hooks/use-endorsements';
import { useDebounce } from '@/hooks/use-debounce';

function BatchStatusBadge({ status }: { status: string }) {
  const variant =
    status === 'COMPLETED'
      ? 'default'
      : status === 'SUBMITTED'
        ? 'secondary'
        : status === 'FAILED'
          ? 'destructive'
          : 'outline';
  return <Badge variant={variant}>{status}</Badge>;
}

export function BatchProgressPage() {
  const [employerId, setEmployerId] = useState(
    '11111111-1111-1111-1111-111111111111',
  );
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const debouncedEmployerId = useDebounce(employerId, 400);

  const { data, isLoading } = useBatchProgress(
    debouncedEmployerId,
    page,
    pageSize,
  );

  return (
    <div className="space-y-6">
      <PageHeader
        title="Batch Progress"
        description="Track endorsement batch submissions and their status"
      />

      <div className="space-y-1">
        <Label className="text-xs">Employer ID</Label>
        <Input
          value={employerId}
          onChange={(e) => {
            setEmployerId(e.target.value);
            setPage(0);
          }}
          className="w-72 text-xs"
        />
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : !data || data.empty ? (
        <EmptyState
          icon={Layers}
          title="No batches found"
          description="No batch submissions found for this employer."
        />
      ) : (
        <>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Batch ID</TableHead>
                  <TableHead>Insurer</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Endorsements</TableHead>
                  <TableHead>Submitted</TableHead>
                  <TableHead>Completed</TableHead>
                  <TableHead>Insurer Ref</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((batch) => (
                  <TableRow key={batch.id}>
                    <TableCell className="font-mono text-xs">
                      {batch.id.substring(0, 8)}...
                    </TableCell>
                    <TableCell className="font-mono text-xs">
                      {batch.insurerId.substring(0, 8)}...
                    </TableCell>
                    <TableCell>
                      <BatchStatusBadge status={batch.status} />
                    </TableCell>
                    <TableCell className="text-right">
                      {batch.endorsementCount}
                    </TableCell>
                    <TableCell
                      className="text-muted-foreground text-xs"
                      title={
                        batch.submittedAt
                          ? new Date(batch.submittedAt).toLocaleString()
                          : undefined
                      }
                    >
                      {batch.submittedAt
                        ? formatDistanceToNow(new Date(batch.submittedAt), {
                            addSuffix: true,
                          })
                        : '--'}
                    </TableCell>
                    <TableCell className="text-muted-foreground text-xs">
                      {batch.completedAt
                        ? format(
                            new Date(batch.completedAt),
                            'dd MMM yyyy HH:mm',
                          )
                        : '--'}
                    </TableCell>
                    <TableCell className="text-xs">
                      {batch.insurerReference || '--'}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <Pagination
            page={page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            pageSize={pageSize}
            onPageChange={setPage}
            onPageSizeChange={(size) => {
              setPageSize(size);
              setPage(0);
            }}
          />
        </>
      )}
    </div>
  );
}
