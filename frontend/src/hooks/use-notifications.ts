import {
  createContext,
  useContext,
  useCallback,
  useReducer,
  type ReactNode,
} from 'react';
import { createElement } from 'react';

export interface Notification {
  id: string;
  type: string;
  title: string;
  message: string;
  endorsementId?: string;
  employerId?: string;
  timestamp: Date;
  read: boolean;
}

interface NotificationState {
  notifications: Notification[];
}

type Action =
  | { type: 'ADD'; notification: Notification }
  | { type: 'ADD_GROUPED'; notifications: Notification[] }
  | { type: 'MARK_READ'; id: string }
  | { type: 'MARK_ALL_READ' };

const MAX_NOTIFICATIONS = 50;
const GROUP_WINDOW_MS = 2000;

function reducer(state: NotificationState, action: Action): NotificationState {
  switch (action.type) {
    case 'ADD': {
      const notifications = [action.notification, ...state.notifications].slice(
        0,
        MAX_NOTIFICATIONS,
      );
      return { notifications };
    }
    case 'ADD_GROUPED': {
      const notifications = [
        ...action.notifications,
        ...state.notifications,
      ].slice(0, MAX_NOTIFICATIONS);
      return { notifications };
    }
    case 'MARK_READ':
      return {
        notifications: state.notifications.map((n) =>
          n.id === action.id ? { ...n, read: true } : n,
        ),
      };
    case 'MARK_ALL_READ':
      return {
        notifications: state.notifications.map((n) => ({ ...n, read: true })),
      };
  }
}

interface NotificationContextValue {
  notifications: Notification[];
  unreadCount: number;
  addNotification: (n: Omit<Notification, 'id' | 'timestamp' | 'read'>) => void;
  markAsRead: (id: string) => void;
  markAllAsRead: () => void;
}

const NotificationContext = createContext<NotificationContextValue | null>(null);

// Grouping buffer
let groupBuffer: Notification[] = [];
let groupTimer: ReturnType<typeof setTimeout> | null = null;

export function NotificationProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, { notifications: [] });

  const addNotification = useCallback(
    (n: Omit<Notification, 'id' | 'timestamp' | 'read'>) => {
      const notification: Notification = {
        ...n,
        id: crypto.randomUUID(),
        timestamp: new Date(),
        read: false,
      };

      groupBuffer.push(notification);

      if (groupTimer) clearTimeout(groupTimer);

      groupTimer = setTimeout(() => {
        const buffered = [...groupBuffer];
        groupBuffer = [];
        groupTimer = null;

        // Group by type within the window
        const byType: Record<string, Notification[]> = {};
        for (const item of buffered) {
          const key = item.type;
          if (!byType[key]) byType[key] = [];
          byType[key].push(item);
        }

        const toAdd: Notification[] = [];
        for (const [type, items] of Object.entries(byType)) {
          if (items.length > 1) {
            // Create a grouped notification
            toAdd.push({
              id: crypto.randomUUID(),
              type,
              title: `${items.length} endorsements ${type.toLowerCase().replace('_', ' ')}`,
              message: `${items.length} endorsement events occurred`,
              employerId: items[0].employerId,
              timestamp: new Date(),
              read: false,
            });
          } else {
            toAdd.push(items[0]);
          }
        }

        dispatch({ type: 'ADD_GROUPED', notifications: toAdd });
      }, GROUP_WINDOW_MS);
    },
    [],
  );

  const markAsRead = useCallback((id: string) => {
    dispatch({ type: 'MARK_READ', id });
  }, []);

  const markAllAsRead = useCallback(() => {
    dispatch({ type: 'MARK_ALL_READ' });
  }, []);

  const unreadCount = state.notifications.filter((n) => !n.read).length;

  return createElement(
    NotificationContext.Provider,
    {
      value: {
        notifications: state.notifications,
        unreadCount,
        addNotification,
        markAsRead,
        markAllAsRead,
      },
    },
    children,
  );
}

export function useNotifications() {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within NotificationProvider');
  }
  return context;
}
