import * as React from 'react';
import { cn } from '@/lib/utils';

interface SlotProps extends React.HTMLAttributes<HTMLElement> {
  children?: React.ReactNode;
}

/**
 * Slot merges its props onto its single child element.
 * Used to implement the `asChild` pattern (like Radix UI's Slot).
 */
const Slot = React.forwardRef<HTMLElement, SlotProps>(
  ({ children, ...props }, ref) => {
    if (React.isValidElement(children)) {
      return React.cloneElement(children, {
        ...mergeProps(props, children.props as Record<string, unknown>),
        ref: ref ?? (children as React.ReactElement<{ ref?: React.Ref<HTMLElement> }>).props?.ref,
      } as Record<string, unknown>);
    }

    if (React.Children.count(children) > 1) {
      React.Children.only(null);
    }

    return null;
  },
);

Slot.displayName = 'Slot';

function mergeProps(
  slotProps: Record<string, unknown>,
  childProps: Record<string, unknown>,
): Record<string, unknown> {
  const merged: Record<string, unknown> = { ...childProps };

  for (const propName in slotProps) {
    const slotPropValue = slotProps[propName];
    const childPropValue = childProps[propName];

    if (propName === 'className') {
      merged[propName] = cn(slotPropValue as string, childPropValue as string);
    } else if (propName === 'style') {
      merged[propName] = { ...(slotPropValue as object), ...(childPropValue as object) };
    } else if (/^on[A-Z]/.test(propName)) {
      if (slotPropValue && childPropValue) {
        merged[propName] = (...args: unknown[]) => {
          (childPropValue as (...args: unknown[]) => void)(...args);
          (slotPropValue as (...args: unknown[]) => void)(...args);
        };
      } else {
        merged[propName] = slotPropValue ?? childPropValue;
      }
    } else {
      merged[propName] = slotPropValue !== undefined ? slotPropValue : childPropValue;
    }
  }

  return merged;
}

export { Slot };
