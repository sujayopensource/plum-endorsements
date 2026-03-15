import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { Menu, Shield, ArrowLeft, Wifi, WifiOff } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetContent,
  SheetTrigger,
  SheetTitle,
} from '@/components/ui/sheet';
import { Sidebar } from './Sidebar';
import { NotificationCenter } from './NotificationCenter';
import { useWebSocket, type ConnectionStatus } from '@/hooks/use-websocket';

function ConnectionIndicator({ status }: { status: ConnectionStatus }) {
  if (status === 'connected') {
    return (
      <span className="flex items-center gap-1 text-xs text-green-600" title="WebSocket connected">
        <Wifi className="h-3 w-3" />
        <span className="hidden sm:inline">Live</span>
      </span>
    );
  }
  return (
    <span className="flex items-center gap-1 text-xs text-muted-foreground" title="WebSocket disconnected">
      <WifiOff className="h-3 w-3" />
    </span>
  );
}

function Breadcrumbs() {
  const location = useLocation();
  const params = useParams();
  const segments = location.pathname.split('/').filter(Boolean);

  const crumbs: Array<{ label: string; href: string }> = [
    { label: 'Dashboard', href: '/' },
  ];

  if (segments[0] === 'endorsements') {
    crumbs.push({ label: 'Endorsements', href: '/endorsements' });
    if (segments[1] === 'new') {
      crumbs.push({ label: 'Create', href: '/endorsements/new' });
    } else if (segments[1] === 'batches') {
      crumbs.push({ label: 'Batches', href: '/endorsements/batches' });
    } else if (params.id) {
      crumbs.push({
        label: `ENS-${params.id.substring(0, 8)}`,
        href: `/endorsements/${params.id}`,
      });
    }
  } else if (segments[0] === 'ea-accounts') {
    crumbs.push({ label: 'EA Accounts', href: '/ea-accounts' });
  } else if (segments[0] === 'intelligence') {
    crumbs.push({ label: 'Intelligence', href: '/intelligence' });
  } else if (segments[0] === 'reconciliation') {
    crumbs.push({ label: 'Reconciliation', href: '/reconciliation' });
  } else if (segments[0] === 'insurers') {
    crumbs.push({ label: 'Insurers', href: '/insurers' });
  }

  return (
    <nav aria-label="Breadcrumb" className="flex items-center gap-1 text-sm">
      {crumbs.map((crumb, i) => (
        <span key={crumb.href} className="flex items-center gap-1">
          {i > 0 && <span className="text-muted-foreground">/</span>}
          {i === crumbs.length - 1 ? (
            <span className="text-foreground font-medium">{crumb.label}</span>
          ) : (
            <Link
              to={crumb.href}
              className="text-muted-foreground hover:text-foreground transition-colors"
            >
              {crumb.label}
            </Link>
          )}
        </span>
      ))}
    </nav>
  );
}

function MobileBackButton() {
  const navigate = useNavigate();
  const location = useLocation();
  const segments = location.pathname.split('/').filter(Boolean);

  if (segments.length <= 1) return null;

  return (
    <Button
      variant="ghost"
      size="icon"
      className="md:hidden"
      onClick={() => navigate(-1)}
      aria-label="Go back"
    >
      <ArrowLeft className="h-5 w-5" />
    </Button>
  );
}

export function TopBar() {
  const { status } = useWebSocket('11111111-1111-1111-1111-111111111111');

  return (
    <header className="border-border bg-background flex h-14 items-center gap-4 border-b px-4 md:px-6">
      <Sheet>
        <SheetTrigger
          className="inline-flex shrink-0 items-center justify-center rounded-lg size-8 hover:bg-muted hover:text-foreground md:hidden"
          aria-label="Open navigation menu"
        >
          <Menu className="h-5 w-5" />
        </SheetTrigger>
        <SheetContent side="left" className="w-64 p-0">
          <SheetTitle className="sr-only">Navigation</SheetTitle>
          <Sidebar mobile />
        </SheetContent>
      </Sheet>
      <MobileBackButton />
      <div className="flex items-center gap-2 md:hidden">
        <Shield className="text-primary h-5 w-5" />
        <span className="font-bold">Plum</span>
      </div>
      <div className="hidden md:block">
        <Breadcrumbs />
      </div>
      <div className="ml-auto flex items-center gap-2">
        <ConnectionIndicator status={status} />
        <NotificationCenter />
      </div>
    </header>
  );
}
