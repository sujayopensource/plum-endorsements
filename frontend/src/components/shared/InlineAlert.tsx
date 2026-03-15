import { AlertTriangle, Info, CheckCircle, XCircle, X } from 'lucide-react';
import { useState } from 'react';

interface InlineAlertProps {
  variant: 'info' | 'warning' | 'error' | 'success';
  title: string;
  message: string;
  dismissible?: boolean;
}

const variantConfig = {
  info: {
    icon: Info,
    border: 'border-l-blue-500',
    bg: 'bg-blue-50',
    iconColor: 'text-blue-600',
  },
  warning: {
    icon: AlertTriangle,
    border: 'border-l-yellow-500',
    bg: 'bg-yellow-50',
    iconColor: 'text-yellow-600',
  },
  error: {
    icon: XCircle,
    border: 'border-l-red-500',
    bg: 'bg-red-50',
    iconColor: 'text-red-600',
  },
  success: {
    icon: CheckCircle,
    border: 'border-l-green-500',
    bg: 'bg-green-50',
    iconColor: 'text-green-600',
  },
};

export function InlineAlert({
  variant,
  title,
  message,
  dismissible = false,
}: InlineAlertProps) {
  const [dismissed, setDismissed] = useState(false);
  const config = variantConfig[variant];
  const Icon = config.icon;

  if (dismissed) return null;

  return (
    <div
      className={`${config.bg} ${config.border} flex items-start gap-3 rounded-md border border-l-4 p-3`}
      role="alert"
    >
      <Icon className={`${config.iconColor} mt-0.5 h-4 w-4 shrink-0`} />
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium">{title}</p>
        <p className="text-muted-foreground text-sm">{message}</p>
      </div>
      {dismissible && (
        <button
          onClick={() => setDismissed(true)}
          className="text-muted-foreground hover:text-foreground shrink-0"
          aria-label="Dismiss alert"
        >
          <X className="h-4 w-4" />
        </button>
      )}
    </div>
  );
}
