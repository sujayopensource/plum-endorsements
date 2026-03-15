import { useState, useEffect, useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  Plus,
  FileText,
  ExternalLink,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  Download,
  RefreshCw,
} from 'lucide-react';
import { format, formatDistanceToNow } from 'date-fns';
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  flexRender,
  type SortingState,
  type ColumnDef,
  type RowSelectionState,
} from '@tanstack/react-table';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
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
import { StatusBadge } from '@/components/shared/StatusBadge';
import { TypeBadge } from '@/components/shared/TypeBadge';
import { Pagination } from '@/components/shared/Pagination';
import { EndorsementFilters } from './components/EndorsementFilters';
import { useEndorsements, useSubmitEndorsement } from '@/hooks/use-endorsements';
import { useDebounce } from '@/hooks/use-debounce';
import { useAnnounce } from '@/hooks/use-announce';
import { exportToCsv } from '@/lib/csv-export';
import type { EndorsementResponse, EndorsementStatus } from '@/types/endorsement';

const SUBMITTABLE_STATUSES: EndorsementStatus[] = [
  'CREATED',
  'VALIDATED',
  'PROVISIONALLY_COVERED',
];

export function EndorsementsListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [sorting, setSorting] = useState<SortingState>([]);
  const [rowSelection, setRowSelection] = useState<RowSelectionState>({});
  const { announce } = useAnnounce();

  // Read filter state from URL
  const employerId =
    searchParams.get('employerId') || '11111111-1111-1111-1111-111111111111';
  const selectedStatuses = searchParams.getAll('statuses') as EndorsementStatus[];
  const page = Number(searchParams.get('page') || '0');
  const pageSize = Number(searchParams.get('size') || '10');

  const debouncedEmployerId = useDebounce(employerId, 400);

  const { data, isLoading, dataUpdatedAt, refetch } = useEndorsements({
    employerId: debouncedEmployerId,
    statuses: selectedStatuses.length > 0 ? selectedStatuses : undefined,
    page,
    size: pageSize,
  });

  const submitMutation = useSubmitEndorsement();

  // Announce results when data loads
  useEffect(() => {
    if (data) {
      announce(`${data.totalElements} endorsements loaded`);
    }
  }, [data, announce]);

  const setEmployerId = (v: string) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      if (v) next.set('employerId', v);
      else next.delete('employerId');
      next.set('page', '0');
      return next;
    });
  };

  const setSelectedStatuses = (statuses: EndorsementStatus[]) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.delete('statuses');
      statuses.forEach((s) => next.append('statuses', s));
      next.set('page', '0');
      return next;
    });
    announce(`Filters applied, ${statuses.length} statuses selected`);
  };

  const setPage = (p: number) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('page', String(p));
      return next;
    });
  };

  const setPageSize = (size: number) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('size', String(size));
      next.set('page', '0');
      return next;
    });
  };

  const columns: ColumnDef<EndorsementResponse>[] = useMemo(
    () => [
      {
        id: 'select',
        header: ({ table }) => (
          <Checkbox
            checked={table.getIsAllPageRowsSelected()}
            onCheckedChange={(value) => table.toggleAllPageRowsSelected(!!value)}
            aria-label="Select all"
          />
        ),
        cell: ({ row }) => {
          const canSelect = SUBMITTABLE_STATUSES.includes(
            row.original.status as EndorsementStatus,
          );
          return (
            <Checkbox
              checked={row.getIsSelected()}
              onCheckedChange={(value) => row.toggleSelected(!!value)}
              disabled={!canSelect}
              aria-label="Select row"
            />
          );
        },
        enableSorting: false,
      },
      {
        accessorKey: 'employeeId',
        header: 'Employee',
        cell: ({ row }) => (
          <span className="font-mono text-xs">
            {row.original.employeeId.substring(0, 8)}...
          </span>
        ),
      },
      {
        accessorKey: 'type',
        header: 'Type',
        cell: ({ row }) => <TypeBadge type={row.original.type} />,
      },
      {
        accessorKey: 'status',
        header: 'Status',
        cell: ({ row }) => <StatusBadge status={row.original.status} />,
      },
      {
        accessorKey: 'premiumAmount',
        header: () => <div className="text-right">Premium</div>,
        cell: ({ row }) => (
          <div className="text-right">
            {row.original.premiumAmount != null
              ? `₹${row.original.premiumAmount.toLocaleString('en-IN')}`
              : '--'}
          </div>
        ),
      },
      {
        accessorKey: 'coverageStartDate',
        header: 'Coverage Start',
        cell: ({ row }) =>
          format(new Date(row.original.coverageStartDate), 'dd MMM yyyy'),
      },
      {
        accessorKey: 'insurerReference',
        header: 'Insurer Ref',
        cell: ({ row }) => (
          <span className="text-xs">
            {row.original.insurerReference || '--'}
          </span>
        ),
      },
      {
        accessorKey: 'createdAt',
        header: 'Created',
        cell: ({ row }) => (
          <span
            className="text-muted-foreground text-xs"
            title={new Date(row.original.createdAt).toLocaleString()}
          >
            {formatDistanceToNow(new Date(row.original.createdAt), {
              addSuffix: true,
            })}
          </span>
        ),
      },
      {
        id: 'actions',
        cell: ({ row }) => (
          <Button
            variant="ghost"
            size="icon"
            asChild
            aria-label="View endorsement details"
          >
            <Link to={`/endorsements/${row.original.id}`}>
              <ExternalLink className="h-4 w-4" />
            </Link>
          </Button>
        ),
        enableSorting: false,
      },
    ],
    [],
  );

  const tableData = data?.content ?? [];

  const table = useReactTable({
    data: tableData,
    columns,
    state: { sorting, rowSelection },
    onSortingChange: setSorting,
    onRowSelectionChange: setRowSelection,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getRowId: (row) => row.id,
    enableRowSelection: (row) =>
      SUBMITTABLE_STATUSES.includes(row.original.status as EndorsementStatus),
  });

  const selectedCount = Object.keys(rowSelection).length;

  const handleBulkSubmit = () => {
    const selectedIds = Object.keys(rowSelection);
    selectedIds.forEach((id) => submitMutation.mutate(id));
    setRowSelection({});
  };

  const handleExportCsv = () => {
    if (!data?.content) return;
    const headers = [
      'Employee ID',
      'Type',
      'Status',
      'Premium',
      'Coverage Start',
      'Insurer Ref',
      'Created',
    ];
    const rows = data.content.map((e) => [
      e.employeeId,
      e.type,
      e.status,
      e.premiumAmount ?? '',
      e.coverageStartDate,
      e.insurerReference ?? '',
      e.createdAt,
    ]);
    exportToCsv('endorsements.csv', headers, rows);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Endorsements"
        description="Manage employee endorsement requests"
        action={
          <div className="flex gap-2">
            <Button variant="outline" onClick={handleExportCsv} disabled={!data?.content?.length}>
              <Download className="mr-2 h-4 w-4" />
              Export CSV
            </Button>
            <Button asChild>
              <Link to="/endorsements/new">
                <Plus className="mr-2 h-4 w-4" />
                Create Endorsement
              </Link>
            </Button>
          </div>
        }
      />

      <EndorsementFilters
        employerId={employerId}
        onEmployerIdChange={setEmployerId}
        selectedStatuses={selectedStatuses}
        onStatusesChange={setSelectedStatuses}
      />

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : !data || data.empty ? (
        <EmptyState
          icon={FileText}
          title="No endorsements found"
          description="Create your first endorsement to get started, or adjust your filters."
          actionLabel="Create Endorsement"
          actionHref="/endorsements/new"
        />
      ) : (
        <>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                {table.getHeaderGroups().map((headerGroup) => (
                  <TableRow key={headerGroup.id}>
                    {headerGroup.headers.map((header) => (
                      <TableHead
                        key={header.id}
                        className={
                          header.column.getCanSort()
                            ? 'cursor-pointer select-none'
                            : ''
                        }
                        onClick={header.column.getToggleSortingHandler()}
                      >
                        <div className="flex items-center gap-1">
                          {header.isPlaceholder
                            ? null
                            : flexRender(
                                header.column.columnDef.header,
                                header.getContext(),
                              )}
                          {header.column.getCanSort() && (
                            <>
                              {header.column.getIsSorted() === 'asc' ? (
                                <ArrowUp className="h-3.5 w-3.5" />
                              ) : header.column.getIsSorted() === 'desc' ? (
                                <ArrowDown className="h-3.5 w-3.5" />
                              ) : (
                                <ArrowUpDown className="h-3.5 w-3.5 opacity-50" />
                              )}
                            </>
                          )}
                        </div>
                      </TableHead>
                    ))}
                  </TableRow>
                ))}
              </TableHeader>
              <TableBody>
                {table.getRowModel().rows.map((row) => (
                  <TableRow
                    key={row.id}
                    data-state={row.getIsSelected() && 'selected'}
                  >
                    {row.getVisibleCells().map((cell) => (
                      <TableCell key={cell.id}>
                        {flexRender(
                          cell.column.columnDef.cell,
                          cell.getContext(),
                        )}
                      </TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              {dataUpdatedAt > 0 && (
                <div className="flex items-center gap-2">
                  <span className="text-muted-foreground text-xs">
                    Updated{' '}
                    {formatDistanceToNow(dataUpdatedAt, { addSuffix: true })}
                  </span>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-6 w-6"
                    onClick={() => refetch()}
                    aria-label="Refresh endorsements"
                  >
                    <RefreshCw className="h-3 w-3" />
                  </Button>
                </div>
              )}
            </div>
          </div>

          <Pagination
            page={page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            pageSize={pageSize}
            onPageChange={setPage}
            onPageSizeChange={setPageSize}
          />
        </>
      )}

      {/* Bulk action bar */}
      {selectedCount > 0 && (
        <div className="bg-background fixed bottom-4 left-1/2 z-50 flex -translate-x-1/2 items-center gap-4 rounded-lg border px-6 py-3 shadow-lg">
          <span className="text-sm font-medium">
            {selectedCount} selected
          </span>
          <Button
            size="sm"
            onClick={handleBulkSubmit}
            disabled={submitMutation.isPending}
          >
            Submit Selected ({selectedCount})
          </Button>
          <button
            className="text-muted-foreground hover:text-foreground text-sm underline"
            onClick={() => setRowSelection({})}
          >
            Clear selection
          </button>
        </div>
      )}
    </div>
  );
}
