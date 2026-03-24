import type { AdminPermission } from '@/types';

export const PLATFORM_COMMISSION = 0.15;

export const SERVICE_TYPES = [
  { id: 'plumbing', label: 'Plumbing', icon: '🔧' },
  { id: 'electrical', label: 'Electrical', icon: '⚡' },
  { id: 'carpentry', label: 'Carpentry', icon: '🪚' },
  { id: 'cleaning', label: 'Cleaning', icon: '🧹' },
  { id: 'painting', label: 'Painting', icon: '🖌️' },
  { id: 'pest_control', label: 'Pest Control', icon: '🐛' },
  { id: 'appliance_repair', label: 'Appliance Repair', icon: '🔌' },
  { id: 'ac_service', label: 'AC Service', icon: '❄️' },
  { id: 'shifting', label: 'Shifting/Moving', icon: '📦' },
  { id: 'beauty', label: 'Beauty & Wellness', icon: '💅' },
] as const;

export const BOOKING_STATUSES = [
  { value: 'pending', label: 'Pending', color: 'yellow' },
  { value: 'confirmed', label: 'Confirmed', color: 'blue' },
  { value: 'in_progress', label: 'In Progress', color: 'purple' },
  { value: 'completed', label: 'Completed', color: 'green' },
  { value: 'cancelled', label: 'Cancelled', color: 'red' },
  { value: 'disputed', label: 'Disputed', color: 'orange' },
] as const;

export const PAYMENT_STATUSES = [
  { value: 'pending', label: 'Pending', color: 'yellow' },
  { value: 'completed', label: 'Completed', color: 'green' },
  { value: 'failed', label: 'Failed', color: 'red' },
  { value: 'refunded', label: 'Refunded', color: 'gray' },
  { value: 'partial_refund', label: 'Partial Refund', color: 'orange' },
] as const;

export const USER_STATUSES = [
  { value: 'active', label: 'Active', color: 'green' },
  { value: 'inactive', label: 'Inactive', color: 'gray' },
  { value: 'suspended', label: 'Suspended', color: 'red' },
  { value: 'pending', label: 'Pending', color: 'yellow' },
] as const;

export const DISPUTE_TYPES = [
  { value: 'payment', label: 'Payment Issue' },
  { value: 'quality', label: 'Quality Issue' },
  { value: 'behaviour', label: 'Behaviour Issue' },
  { value: 'no_show', label: 'No Show' },
  { value: 'fraud', label: 'Fraud' },
  { value: 'other', label: 'Other' },
] as const;

export const DISPUTE_PRIORITIES = [
  { value: 'low', label: 'Low', color: 'green' },
  { value: 'medium', label: 'Medium', color: 'yellow' },
  { value: 'high', label: 'High', color: 'orange' },
  { value: 'urgent', label: 'Urgent', color: 'red' },
] as const;

export const ADMIN_ROLES = [
  { value: 'super_admin', label: 'Super Admin' },
  { value: 'admin', label: 'Admin' },
  { value: 'moderator', label: 'Moderator' },
  { value: 'support', label: 'Support' },
] as const;

export const ALL_PERMISSIONS: { value: AdminPermission; label: string }[] = [
  { value: 'manage_users', label: 'Manage Users' },
  { value: 'manage_bookings', label: 'Manage Bookings' },
  { value: 'manage_payments', label: 'Manage Payments' },
  { value: 'manage_services', label: 'Manage Services' },
  { value: 'manage_reviews', label: 'Manage Reviews' },
  { value: 'manage_disputes', label: 'Manage Disputes' },
  { value: 'send_notifications', label: 'Send Notifications' },
  { value: 'view_analytics', label: 'View Analytics' },
  { value: 'manage_settings', label: 'Manage Settings' },
  { value: 'manage_admins', label: 'Manage Admins' },
];

export const PAYMENT_METHODS = [
  { value: 'upi', label: 'UPI' },
  { value: 'card', label: 'Card' },
  { value: 'netbanking', label: 'Net Banking' },
  { value: 'wallet', label: 'Wallet' },
  { value: 'cash', label: 'Cash' },
] as const;

export const OPERATING_CITIES = [
  'Mumbai',
  'Delhi',
  'Bangalore',
  'Hyderabad',
  'Chennai',
  'Kolkata',
  'Pune',
  'Ahmedabad',
  'Jaipur',
  'Surat',
  'Lucknow',
  'Nagpur',
];

export const NAV_ITEMS = [
  { href: '/dashboard', label: 'Dashboard', icon: 'LayoutDashboard' },
  {
    label: 'Users',
    icon: 'Users',
    children: [
      { href: '/users/customers', label: 'Customers', icon: 'User' },
      { href: '/users/providers', label: 'Providers', icon: 'Briefcase' },
    ],
  },
  { href: '/bookings', label: 'Bookings', icon: 'Calendar' },
  { href: '/payments', label: 'Payments', icon: 'CreditCard' },
  { href: '/services', label: 'Services', icon: 'Tool' },
  { href: '/reviews', label: 'Reviews', icon: 'Star' },
  { href: '/disputes', label: 'Disputes', icon: 'AlertTriangle' },
  { href: '/analytics', label: 'Analytics', icon: 'BarChart2' },
  { href: '/notifications', label: 'Notifications', icon: 'Bell' },
  { href: '/settings', label: 'Settings', icon: 'Settings' },
  { href: '/admins', label: 'Admins', icon: 'Shield' },
] as const;

export const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];
export const DEFAULT_PAGE_SIZE = 25;
