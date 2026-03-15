import { useState } from 'react';
import { Link } from 'react-router-dom';
import { formatDistanceToNow } from 'date-fns';
import {
  FileText,
  Clock,
  CheckCircle,
  XCircle,
  ArrowRight,
  Wallet,
  RefreshCw,
  Layers,
  AlertCircle,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { StatusBadge } from '@/components/shared/StatusBadge';
import { TypeBadge } from '@/components/shared/TypeBadge';
import { PageHeader } from '@/components/shared/PageHeader';
import { useEndorsements, useBatchProgress, useOutstandingItems } from '@/hooks/use-endorsements';
import { useEAAccount } from '@/hooks/use-ea-account';
import { ACTIVE_STATUSES, STATUS_CONFIG } from '@/lib/constants';
import { format } from 'date-fns';

export function DashboardPage() {
  const [employerId, setEmployerId] = useState(
    '11111111-1111-1111-1111-111111111111',
  );
  const [insurerId, setInsurerId] = useState(
    '22222222-2222-2222-2222-222222222222',
  );

  const { data: allData, isLoading, dataUpdatedAt, refetch } = useEndorsements({
    employerId,
    page: 0,
    size: 100,
  });

  const { data: eaAccount } = useEAAccount(employerId, insurerId);
  const { data: batchData } = useBatchProgress(employerId, 0, 5);
  const { data: outstandingData } = useOutstandingItems(employerId, 0, 5);

  const endorsements = allData?.content ?? [];
  const total = allData?.totalElements ?? 0;
  const pending = endorsements.filter((e) =>
    ACTIVE_STATUSES.includes(e.status),
  ).length;
  const confirmed = endorsements.filter(
    (e) => e.status === 'CONFIRMED',
  ).length;
  const failed = endorsements.filter(
    (e) => e.status === 'REJECTED' || e.status === 'FAILED_PERMANENT',
  ).length;
  const recent = endorsements.slice(0, 5);

  const statusCounts: Record<string, number> = {};
  for (const e of endorsements) {
    statusCounts[e.status] = (statusCounts[e.status] ?? 0) + 1;
  }

  const activeBatchCount = batchData?.content?.filter(
    (b) => b.status !== 'COMPLETED' && b.status !== 'FAILED',
  ).length ?? 0;
  const outstandingCount = outstandingData?.totalElements ?? 0;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        description="Overview of endorsement operations"
      />

      <div className="flex flex-wrap items-end gap-4">
        <div className="space-y-1">
          <Label className="text-xs">Employer ID</Label>
          <Input
            value={employerId}
            onChange={(e) => setEmployerId(e.target.value)}
            className="w-72 text-xs"
          />
        </div>
        <div className="space-y-1">
          <Label className="text-xs">Insurer ID</Label>
          <Input
            value={insurerId}
            onChange={(e) => setInsurerId(e.target.value)}
            className="w-72 text-xs"
          />
        </div>
      </div>

      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-28" />
          ))}
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-4">
          <Link
            to={`/endorsements?employerId=${encodeURIComponent(employerId)}`}
            className="transition-shadow hover:shadow-md"
          >
            <Card className="cursor-pointer">
              <CardContent className="flex items-center gap-4 pt-6">
                <div className="rounded-lg bg-blue-100 p-3">
                  <FileText className="h-5 w-5 text-blue-600" />
                </div>
                <div>
                  <p className="text-muted-foreground text-sm">Total</p>
                  <p className="text-2xl font-bold">{total}</p>
                </div>
              </CardContent>
            </Card>
          </Link>
          <Link
            to={`/endorsements?employerId=${encodeURIComponent(employerId)}&${ACTIVE_STATUSES.map((s) => `statuses=${s}`).join('&')}`}
            className="transition-shadow hover:shadow-md"
          >
            <Card className="cursor-pointer">
              <CardContent className="flex items-center gap-4 pt-6">
                <div className="rounded-lg bg-yellow-100 p-3">
                  <Clock className="h-5 w-5 text-yellow-600" />
                </div>
                <div>
                  <p className="text-muted-foreground text-sm">Pending</p>
                  <p className="text-2xl font-bold">{pending}</p>
                </div>
              </CardContent>
            </Card>
          </Link>
          <Link
            to={`/endorsements?employerId=${encodeURIComponent(employerId)}&statuses=CONFIRMED`}
            className="transition-shadow hover:shadow-md"
          >
            <Card className="cursor-pointer">
              <CardContent className="flex items-center gap-4 pt-6">
                <div className="rounded-lg bg-green-100 p-3">
                  <CheckCircle className="h-5 w-5 text-green-600" />
                </div>
                <div>
                  <p className="text-muted-foreground text-sm">Confirmed</p>
                  <p className="text-2xl font-bold">{confirmed}</p>
                </div>
              </CardContent>
            </Card>
          </Link>
          <Link
            to={`/endorsements?employerId=${encodeURIComponent(employerId)}&statuses=REJECTED&statuses=FAILED_PERMANENT`}
            className="transition-shadow hover:shadow-md"
          >
            <Card className="cursor-pointer">
              <CardContent className="flex items-center gap-4 pt-6">
                <div className="rounded-lg bg-red-100 p-3">
                  <XCircle className="h-5 w-5 text-red-600" />
                </div>
                <div>
                  <p className="text-muted-foreground text-sm">Failed</p>
                  <p className="text-2xl font-bold">{failed}</p>
                </div>
              </CardContent>
            </Card>
          </Link>
        </div>
      )}

      {/* Batch & Outstanding summary cards */}
      <div className="grid gap-4 md:grid-cols-2">
        <Link to="/endorsements/batches" className="transition-shadow hover:shadow-md">
          <Card className="cursor-pointer">
            <CardContent className="flex items-center gap-4 pt-6">
              <div className="rounded-lg bg-indigo-100 p-3">
                <Layers className="h-5 w-5 text-indigo-600" />
              </div>
              <div>
                <p className="text-muted-foreground text-sm">Active Batches</p>
                <p className="text-2xl font-bold">{activeBatchCount}</p>
              </div>
              <ArrowRight className="text-muted-foreground ml-auto h-4 w-4" />
            </CardContent>
          </Card>
        </Link>
        <Card>
          <CardContent className="flex items-center gap-4 pt-6">
            <div className="rounded-lg bg-orange-100 p-3">
              <AlertCircle className="h-5 w-5 text-orange-600" />
            </div>
            <div>
              <p className="text-muted-foreground text-sm">Outstanding Items</p>
              <p className="text-2xl font-bold">{outstandingCount}</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {endorsements.length > 0 && (
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-base">Status Distribution</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex h-4 overflow-hidden rounded-full">
              {Object.entries(statusCounts).map(([status, count]) => {
                const pct = (count / endorsements.length) * 100;
                const config =
                  STATUS_CONFIG[status as keyof typeof STATUS_CONFIG];
                return (
                  <div
                    key={status}
                    className={`${config.className} border-0`}
                    style={{ width: `${pct}%` }}
                    title={`${config.label}: ${count}`}
                  />
                );
              })}
            </div>
            <div className="mt-3 flex flex-wrap gap-3">
              {Object.entries(statusCounts).map(([status, count]) => {
                const config =
                  STATUS_CONFIG[status as keyof typeof STATUS_CONFIG];
                return (
                  <span
                    key={status}
                    className="text-muted-foreground text-xs"
                  >
                    {config.label}: {count}
                  </span>
                );
              })}
            </div>
          </CardContent>
        </Card>
      )}

      <div className="grid gap-6 md:grid-cols-3">
        <Card className="md:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <CardTitle className="text-base">Recent Endorsements</CardTitle>
            <Button variant="ghost" size="sm" asChild aria-label="View all endorsements">
              <Link to="/endorsements">
                View all <ArrowRight className="ml-1 h-4 w-4" />
              </Link>
            </Button>
          </CardHeader>
          <CardContent>
            {recent.length === 0 ? (
              <p className="text-muted-foreground py-4 text-center text-sm">
                No endorsements yet
              </p>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Employee</TableHead>
                    <TableHead>Type</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Created</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {recent.map((e) => (
                    <TableRow key={e.id}>
                      <TableCell className="font-mono text-xs">
                        <Link
                          to={`/endorsements/${e.id}`}
                          className="hover:underline"
                        >
                          {e.employeeId.substring(0, 8)}...
                        </Link>
                      </TableCell>
                      <TableCell>
                        <TypeBadge type={e.type} />
                      </TableCell>
                      <TableCell>
                        <StatusBadge status={e.status} />
                      </TableCell>
                      <TableCell className="text-muted-foreground text-xs">
                        {formatDistanceToNow(new Date(e.createdAt), {
                          addSuffix: true,
                        })}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base">
              <Wallet className="h-4 w-4" />
              EA Account
            </CardTitle>
          </CardHeader>
          <CardContent>
            {eaAccount ? (
              <div className="space-y-4">
                <div>
                  <p className="text-muted-foreground text-xs">Balance</p>
                  <p className="text-xl font-bold">
                    ₹{eaAccount.balance.toLocaleString('en-IN')}
                  </p>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <p className="text-muted-foreground text-xs">Reserved</p>
                    <p className="text-lg font-semibold text-amber-600">
                      ₹{eaAccount.reserved.toLocaleString('en-IN')}
                    </p>
                  </div>
                  <div>
                    <p className="text-muted-foreground text-xs">Available</p>
                    <p className="text-lg font-semibold text-green-600">
                      ₹{eaAccount.availableBalance.toLocaleString('en-IN')}
                    </p>
                  </div>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-gray-100">
                  <div
                    className="h-full bg-green-500"
                    style={{
                      width: `${(eaAccount.availableBalance / eaAccount.balance) * 100}%`,
                    }}
                  />
                </div>
                <p className="text-muted-foreground text-xs">
                  Last updated:{' '}
                  {format(new Date(eaAccount.updatedAt), 'dd MMM yyyy HH:mm')}
                </p>
              </div>
            ) : (
              <p className="text-muted-foreground py-4 text-center text-sm">
                No EA account found
              </p>
            )}
          </CardContent>
        </Card>
      </div>

      {dataUpdatedAt > 0 && (
        <div className="flex items-center gap-2">
          <span className="text-muted-foreground text-xs">
            Dashboard updated{' '}
            {formatDistanceToNow(dataUpdatedAt, { addSuffix: true })}
          </span>
          <Button
            variant="ghost"
            size="icon"
            className="h-6 w-6"
            onClick={() => refetch()}
            aria-label="Refresh dashboard"
          >
            <RefreshCw className="h-3 w-3" />
          </Button>
        </div>
      )}
    </div>
  );
}
