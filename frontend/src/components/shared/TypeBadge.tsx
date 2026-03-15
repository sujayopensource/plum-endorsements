import { Badge } from '@/components/ui/badge';
import { TYPE_CONFIG } from '@/lib/constants';
import type { EndorsementType } from '@/types/endorsement';

export function TypeBadge({ type }: { type: EndorsementType }) {
  const config = TYPE_CONFIG[type];
  return (
    <Badge variant="outline" className={config.className}>
      {config.label}
    </Badge>
  );
}
