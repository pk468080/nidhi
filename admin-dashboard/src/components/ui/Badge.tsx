'use client';

import { cn } from '@/utils/helpers';

interface BadgeProps {
  children: React.ReactNode;
  className?: string;
  variant?: 'default' | 'success' | 'warning' | 'error' | 'info' | 'purple';
}

const variantClasses: Record<string, string> = {
  default: 'bg-gray-100 text-gray-800',
  success: 'bg-green-100 text-green-800',
  warning: 'bg-yellow-100 text-yellow-800',
  error: 'bg-red-100 text-red-800',
  info: 'bg-blue-100 text-blue-800',
  purple: 'bg-purple-100 text-purple-800',
};

export function Badge({ children, className, variant = 'default' }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        variantClasses[variant],
        className
      )}
    >
      {children}
    </span>
  );
}

export function StatusBadge({ status, className }: { status: string; className?: string }) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium capitalize',
        getStatusVariant(status),
        className
      )}
    >
      {status.replace(/_/g, ' ')}
    </span>
  );
}

function getStatusVariant(status: string): string {
  const map: Record<string, string> = {
    active: 'bg-green-100 text-green-800',
    completed: 'bg-green-100 text-green-800',
    published: 'bg-green-100 text-green-800',
    resolved: 'bg-green-100 text-green-800',
    sent: 'bg-green-100 text-green-800',
    pending: 'bg-yellow-100 text-yellow-800',
    confirmed: 'bg-blue-100 text-blue-800',
    under_review: 'bg-yellow-100 text-yellow-800',
    scheduled: 'bg-purple-100 text-purple-800',
    in_progress: 'bg-purple-100 text-purple-800',
    cancelled: 'bg-red-100 text-red-800',
    suspended: 'bg-red-100 text-red-800',
    failed: 'bg-red-100 text-red-800',
    open: 'bg-red-100 text-red-800',
    disputed: 'bg-orange-100 text-orange-800',
    escalated: 'bg-orange-100 text-orange-800',
    inactive: 'bg-gray-100 text-gray-800',
    hidden: 'bg-gray-100 text-gray-800',
    closed: 'bg-gray-100 text-gray-800',
    refunded: 'bg-gray-100 text-gray-800',
    flagged: 'bg-red-100 text-red-800',
  };
  return map[status] ?? 'bg-gray-100 text-gray-800';
}
