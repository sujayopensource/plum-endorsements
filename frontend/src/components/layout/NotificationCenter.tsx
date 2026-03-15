import { useNavigate } from 'react-router-dom';
import { Bell, CheckCheck } from 'lucide-react';
import { formatDistanceToNow, isToday, isYesterday } from 'date-fns';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { ScrollArea } from '@/components/ui/scroll-area';
import { useNotifications } from '@/hooks/use-notifications';

function groupByDate(
  notifications: { timestamp: Date }[],
): { label: string; items: typeof notifications }[] {
  const today: typeof notifications = [];
  const yesterday: typeof notifications = [];
  const older: typeof notifications = [];

  for (const n of notifications) {
    if (isToday(n.timestamp)) today.push(n);
    else if (isYesterday(n.timestamp)) yesterday.push(n);
    else older.push(n);
  }

  const groups: { label: string; items: typeof notifications }[] = [];
  if (today.length) groups.push({ label: 'Today', items: today });
  if (yesterday.length) groups.push({ label: 'Yesterday', items: yesterday });
  if (older.length) groups.push({ label: 'Older', items: older });
  return groups;
}

export function NotificationCenter() {
  const { notifications, unreadCount, markAsRead, markAllAsRead } =
    useNotifications();
  const navigate = useNavigate();

  const groups = groupByDate(notifications);

  return (
    <Popover>
      <PopoverTrigger
        className="relative inline-flex shrink-0 items-center justify-center rounded-lg text-sm font-medium hover:bg-muted hover:text-foreground size-8"
        aria-label="Notifications"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <Badge
            variant="destructive"
            className="absolute -top-1 -right-1 h-5 min-w-5 px-1 text-[10px]"
          >
            {unreadCount > 99 ? '99+' : unreadCount}
          </Badge>
        )}
      </PopoverTrigger>
      <PopoverContent className="w-80 p-0" align="end">
        <div className="flex items-center justify-between border-b px-4 py-3">
          <h3 className="text-sm font-semibold">Notifications</h3>
          {unreadCount > 0 && (
            <Button
              variant="ghost"
              size="sm"
              className="h-auto px-2 py-1 text-xs"
              onClick={markAllAsRead}
            >
              <CheckCheck className="mr-1 h-3 w-3" />
              Mark all read
            </Button>
          )}
        </div>
        <ScrollArea className="max-h-[400px]">
          {notifications.length === 0 ? (
            <p className="text-muted-foreground py-8 text-center text-sm">
              No notifications yet
            </p>
          ) : (
            <div className="divide-y">
              {groups.map((group) => (
                <div key={group.label}>
                  <p className="text-muted-foreground bg-muted/50 px-4 py-1.5 text-xs font-medium">
                    {group.label}
                  </p>
                  {group.items.map((notification) => {
                    const n = notification as (typeof notifications)[0];
                    return (
                      <button
                        key={n.id}
                        className={`flex w-full gap-3 px-4 py-3 text-left transition-colors hover:bg-muted/50 ${
                          !n.read ? 'bg-blue-50/50' : ''
                        }`}
                        onClick={() => {
                          markAsRead(n.id);
                          if (n.endorsementId) {
                            navigate(`/endorsements/${n.endorsementId}`);
                          }
                        }}
                      >
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium truncate">
                            {n.title}
                          </p>
                          <p className="text-muted-foreground text-xs truncate">
                            {n.message}
                          </p>
                          <p className="text-muted-foreground mt-1 text-[10px]">
                            {formatDistanceToNow(n.timestamp, {
                              addSuffix: true,
                            })}
                          </p>
                        </div>
                        {!n.read && (
                          <div className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-blue-500" />
                        )}
                      </button>
                    );
                  })}
                </div>
              ))}
            </div>
          )}
        </ScrollArea>
      </PopoverContent>
    </Popover>
  );
}
