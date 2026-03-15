import { Link, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  FileText,
  Wallet,
  Building2,
  GitCompare,
  Shield,
  Sparkles,
  Layers,
} from 'lucide-react';
import { cn } from '@/lib/utils';

const navItems = [
  { label: 'Dashboard', href: '/', icon: LayoutDashboard },
  { label: 'Endorsements', href: '/endorsements', icon: FileText },
  { label: 'Batches', href: '/endorsements/batches', icon: Layers },
  { label: 'EA Accounts', href: '/ea-accounts', icon: Wallet },
  { label: 'Insurers', href: '/insurers', icon: Building2 },
  { label: 'Reconciliation', href: '/reconciliation', icon: GitCompare },
  { label: 'Intelligence', href: '/intelligence', icon: Sparkles },
];

export function Sidebar({ mobile = false }: { mobile?: boolean }) {
  const location = useLocation();

  return (
    <aside
      className={cn(
        'border-border bg-sidebar w-64 flex-col border-r',
        mobile ? 'flex h-full' : 'hidden md:flex',
      )}
    >
      <div className="border-border flex h-14 items-center gap-2 border-b px-4">
        <Shield className="text-primary h-6 w-6" />
        <span className="text-lg font-bold">Plum</span>
        <span className="text-muted-foreground text-sm">Endorsements</span>
      </div>
      <nav className="flex-1 space-y-1 p-3">
        {navItems.map((item) => {
          const isActive =
            item.href === '/'
              ? location.pathname === '/'
              : item.href === '/endorsements'
                ? location.pathname === '/endorsements' ||
                  (location.pathname.startsWith('/endorsements/') &&
                    !location.pathname.startsWith('/endorsements/batches'))
                : location.pathname.startsWith(item.href);
          return (
            <Link
              key={item.href}
              to={item.href}
              className={cn(
                'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-sidebar-accent text-sidebar-accent-foreground'
                  : 'text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-accent-foreground',
              )}
            >
              <item.icon className="h-4 w-4" />
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
