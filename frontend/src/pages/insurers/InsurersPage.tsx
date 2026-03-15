import { Link } from 'react-router-dom';
import { Building2, ExternalLink } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
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

export function InsurersPage() {
  const { data: insurers, isLoading } = useInsurers();

  return (
    <div className="space-y-6">
      <PageHeader
        title="Insurer Configurations"
        description="View and manage insurer adapter configurations"
      />

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : !insurers || insurers.length === 0 ? (
        <EmptyState
          icon={Building2}
          title="No insurers found"
          description="No insurer configurations have been registered yet."
        />
      ) : (
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Code</TableHead>
                <TableHead>Adapter Type</TableHead>
                <TableHead>Data Format</TableHead>
                <TableHead>Real-time?</TableHead>
                <TableHead>Batch?</TableHead>
                <TableHead className="text-right">Rate Limit</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="w-10" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {insurers.map((insurer) => (
                <TableRow key={insurer.insurerId}>
                  <TableCell className="font-medium">
                    {insurer.insurerName}
                  </TableCell>
                  <TableCell className="font-mono text-xs">
                    {insurer.insurerCode}
                  </TableCell>
                  <TableCell>{insurer.adapterType}</TableCell>
                  <TableCell className="text-xs">
                    {insurer.dataFormat}
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant={insurer.supportsRealTime ? 'default' : 'outline'}
                    >
                      {insurer.supportsRealTime ? 'Yes' : 'No'}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant={insurer.supportsBatch ? 'default' : 'outline'}
                    >
                      {insurer.supportsBatch ? 'Yes' : 'No'}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    {insurer.rateLimitPerMinute}/min
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant={insurer.active ? 'default' : 'secondary'}
                    >
                      {insurer.active ? 'Active' : 'Inactive'}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Link to={`/insurers/${insurer.insurerId}`}>
                      <ExternalLink className="h-4 w-4 text-muted-foreground hover:text-foreground" />
                    </Link>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
