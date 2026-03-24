import { format, formatDistanceToNow, parseISO } from 'date-fns';

export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(amount);
}

export function formatDate(date: Date | string | undefined | null): string {
  if (!date) return 'N/A';
  try {
    const d = typeof date === 'string' ? parseISO(date) : date;
    return format(d, 'dd MMM yyyy');
  } catch {
    return 'Invalid date';
  }
}

export function formatDateTime(date: Date | string | undefined | null): string {
  if (!date) return 'N/A';
  try {
    const d = typeof date === 'string' ? parseISO(date) : date;
    return format(d, 'dd MMM yyyy, hh:mm a');
  } catch {
    return 'Invalid date';
  }
}

export function formatRelativeTime(date: Date | string | undefined | null): string {
  if (!date) return 'N/A';
  try {
    const d = typeof date === 'string' ? parseISO(date) : date;
    return formatDistanceToNow(d, { addSuffix: true });
  } catch {
    return 'N/A';
  }
}

export function formatPhoneNumber(phone: string | undefined | null): string {
  if (!phone) return 'N/A';
  const cleaned = phone.replace(/\D/g, '');
  if (cleaned.length === 10) {
    return `+91 ${cleaned.slice(0, 5)} ${cleaned.slice(5)}`;
  }
  if (cleaned.length === 12 && cleaned.startsWith('91')) {
    return `+91 ${cleaned.slice(2, 7)} ${cleaned.slice(7)}`;
  }
  return phone;
}

export function getStatusColor(status: string): string {
  const statusColors: Record<string, string> = {
    // Booking statuses
    pending: 'bg-yellow-100 text-yellow-800',
    confirmed: 'bg-blue-100 text-blue-800',
    in_progress: 'bg-purple-100 text-purple-800',
    completed: 'bg-green-100 text-green-800',
    cancelled: 'bg-red-100 text-red-800',
    disputed: 'bg-orange-100 text-orange-800',
    // Payment statuses
    failed: 'bg-red-100 text-red-800',
    refunded: 'bg-gray-100 text-gray-800',
    partial_refund: 'bg-orange-100 text-orange-800',
    // User statuses
    active: 'bg-green-100 text-green-800',
    inactive: 'bg-gray-100 text-gray-800',
    suspended: 'bg-red-100 text-red-800',
    // Review statuses
    published: 'bg-green-100 text-green-800',
    hidden: 'bg-gray-100 text-gray-800',
    flagged: 'bg-red-100 text-red-800',
    // Dispute statuses
    open: 'bg-red-100 text-red-800',
    under_review: 'bg-yellow-100 text-yellow-800',
    resolved: 'bg-green-100 text-green-800',
    closed: 'bg-gray-100 text-gray-800',
    escalated: 'bg-orange-100 text-orange-800',
    // Priority
    low: 'bg-green-100 text-green-800',
    medium: 'bg-yellow-100 text-yellow-800',
    high: 'bg-orange-100 text-orange-800',
    urgent: 'bg-red-100 text-red-800',
    // Notification types
    draft: 'bg-gray-100 text-gray-800',
    sent: 'bg-blue-100 text-blue-800',
    scheduled: 'bg-purple-100 text-purple-800',
  };
  return statusColors[status] ?? 'bg-gray-100 text-gray-800';
}

export function getPriorityColor(priority: string): string {
  const priorityColors: Record<string, string> = {
    low: 'bg-green-100 text-green-800',
    medium: 'bg-yellow-100 text-yellow-800',
    high: 'bg-orange-100 text-orange-800',
    urgent: 'bg-red-100 text-red-800',
  };
  return priorityColors[priority] ?? 'bg-gray-100 text-gray-800';
}

export function formatPercentage(value: number, decimals = 1): string {
  return `${value >= 0 ? '+' : ''}${value.toFixed(decimals)}%`;
}

export function formatNumber(value: number): string {
  return new Intl.NumberFormat('en-IN').format(value);
}

export function truncateText(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text;
  return `${text.slice(0, maxLength)}...`;
}
