import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { Filter, X } from 'lucide-react';
import { ALL_STATUSES, STATUS_CONFIG } from '@/lib/constants';
import type { EndorsementStatus } from '@/types/endorsement';

interface EndorsementFiltersProps {
  employerId: string;
  onEmployerIdChange: (value: string) => void;
  selectedStatuses: EndorsementStatus[];
  onStatusesChange: (statuses: EndorsementStatus[]) => void;
}

export function EndorsementFilters({
  employerId,
  onEmployerIdChange,
  selectedStatuses,
  onStatusesChange,
}: EndorsementFiltersProps) {
  const toggleStatus = (status: EndorsementStatus) => {
    if (selectedStatuses.includes(status)) {
      onStatusesChange(selectedStatuses.filter((s) => s !== status));
    } else {
      onStatusesChange([...selectedStatuses, status]);
    }
  };

  return (
    <div className="flex flex-wrap items-center gap-3">
      <Input
        placeholder="Employer ID (UUID)"
        value={employerId}
        onChange={(e) => onEmployerIdChange(e.target.value)}
        className="w-80"
      />
      <Popover>
        <PopoverTrigger asChild>
          <Button variant="outline" size="sm" className="gap-2">
            <Filter className="h-4 w-4" />
            Status
            {selectedStatuses.length > 0 && (
              <Badge variant="secondary" className="ml-1 px-1.5">
                {selectedStatuses.length}
              </Badge>
            )}
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-56 p-2" align="start">
          <div className="space-y-1">
            {ALL_STATUSES.map((status) => (
              <button
                key={status}
                onClick={() => toggleStatus(status)}
                className={`flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm transition-colors ${
                  selectedStatuses.includes(status)
                    ? 'bg-accent'
                    : 'hover:bg-accent/50'
                }`}
              >
                <div
                  className={`h-3 w-3 rounded-sm border ${
                    selectedStatuses.includes(status)
                      ? 'border-primary bg-primary'
                      : 'border-input'
                  }`}
                />
                {STATUS_CONFIG[status].label}
              </button>
            ))}
          </div>
          {selectedStatuses.length > 0 && (
            <Button
              variant="ghost"
              size="sm"
              className="mt-2 w-full"
              onClick={() => onStatusesChange([])}
            >
              <X className="mr-1 h-3 w-3" />
              Clear filters
            </Button>
          )}
        </PopoverContent>
      </Popover>
    </div>
  );
}
