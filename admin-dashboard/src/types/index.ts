export interface User {
  id: string;
  name: string;
  email: string;
  phone: string;
  role: 'customer' | 'provider' | 'admin';
  status: 'active' | 'inactive' | 'suspended' | 'pending';
  createdAt: Date | string;
  lastActive: Date | string;
  bookingCount: number;
  totalSpent: number;
  rating: number;
  profilePicture?: string;
  city?: string;
}

export interface Provider extends User {
  serviceType: string;
  city: string;
  completedBookings: number;
  earnings: number;
  avgRating: number;
  reviewCount: number;
  isVerified: boolean;
  responseTime: number; // minutes
  bio?: string;
  documents?: string[];
  bankAccount?: {
    accountNumber: string;
    ifsc: string;
    name: string;
  };
}

export interface Booking {
  id: string;
  customerId: string;
  customerName: string;
  customerPhone?: string;
  providerId: string;
  providerName: string;
  serviceType: string;
  amount: number;
  status: 'pending' | 'confirmed' | 'in_progress' | 'completed' | 'cancelled' | 'disputed';
  createdAt: Date | string;
  scheduledAt: Date | string;
  completedAt?: Date | string;
  rating?: number;
  review?: string;
  address?: string;
  notes?: string;
  cancellationReason?: string;
}

export interface Payment {
  id: string;
  bookingId: string;
  customerId: string;
  customerName: string;
  providerId?: string;
  providerName?: string;
  amount: number;
  method: 'upi' | 'card' | 'netbanking' | 'wallet' | 'cash';
  status: 'pending' | 'completed' | 'failed' | 'refunded' | 'partial_refund';
  createdAt: Date | string;
  providerPayout: number;
  platformFee: number;
  transactionId?: string;
  refundAmount?: number;
}

export interface Service {
  id: string;
  name: string;
  description: string;
  icon: string;
  basePrice: number;
  isEnabled: boolean;
  bookingCount: number;
  category?: string;
  priceUnit?: string;
  minPrice?: number;
  maxPrice?: number;
}

export interface Review {
  id: string;
  customerId: string;
  customerName: string;
  providerId: string;
  providerName: string;
  bookingId: string;
  serviceType: string;
  rating: number;
  text: string;
  createdAt: Date | string;
  status: 'published' | 'hidden' | 'pending' | 'flagged';
  adminNote?: string;
  providerReply?: string;
}

export interface Dispute {
  id: string;
  reporterId: string;
  reporterName: string;
  reporterRole: 'customer' | 'provider';
  againstId?: string;
  againstName?: string;
  bookingId?: string;
  type: 'payment' | 'quality' | 'behaviour' | 'no_show' | 'fraud' | 'other';
  subject: string;
  description: string;
  status: 'open' | 'under_review' | 'resolved' | 'closed' | 'escalated';
  priority: 'low' | 'medium' | 'high' | 'urgent';
  createdAt: Date | string;
  resolvedAt?: Date | string;
  resolution?: string;
  assignedTo?: string;
  attachments?: string[];
}

export interface Notification {
  id: string;
  title: string;
  body: string;
  type: 'promotional' | 'transactional' | 'alert' | 'update';
  targetAudience: 'all' | 'customers' | 'providers' | 'specific';
  targetIds?: string[];
  sentAt: Date | string;
  readCount: number;
  totalSent: number;
  status: 'draft' | 'sent' | 'scheduled';
  scheduledAt?: Date | string;
  imageUrl?: string;
  actionUrl?: string;
}

export interface AdminUser {
  id: string;
  email: string;
  name: string;
  role: 'super_admin' | 'admin' | 'moderator' | 'support';
  permissions: AdminPermission[];
  createdAt: Date | string;
  lastLogin: Date | string;
  isActive: boolean;
  profilePicture?: string;
  phone?: string;
}

export type AdminPermission =
  | 'manage_users'
  | 'manage_bookings'
  | 'manage_payments'
  | 'manage_services'
  | 'manage_reviews'
  | 'manage_disputes'
  | 'send_notifications'
  | 'view_analytics'
  | 'manage_settings'
  | 'manage_admins';

export interface DashboardMetrics {
  totalUsers: number;
  totalBookings: number;
  totalRevenue: number;
  activeProviders: number;
  pendingApprovals: number;
  avgRating: number;
  todayBookings: number;
  todayRevenue: number;
  monthlyGrowth: {
    users: number;
    bookings: number;
    revenue: number;
  };
}

export interface RevenueData {
  date: string;
  revenue: number;
  bookings: number;
  commission: number;
}

export interface Column<T> {
  key: keyof T | string;
  header: string;
  render?: (value: unknown, row: T) => React.ReactNode;
  sortable?: boolean;
  width?: string;
}

export interface PaginationState {
  page: number;
  pageSize: number;
  total: number;
}

export interface FilterState {
  search: string;
  status?: string;
  dateFrom?: string;
  dateTo?: string;
  serviceType?: string;
  [key: string]: string | undefined;
}
