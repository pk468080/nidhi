import { type ClassValue, clsx } from 'clsx';
import type { Booking, Provider, User, Payment } from '@/types';

export function cn(...inputs: ClassValue[]) {
  return clsx(inputs);
}

export function generateId(): string {
  return Math.random().toString(36).slice(2, 11);
}

export function debounce<T extends (...args: unknown[]) => unknown>(fn: T, delay: number) {
  let timer: NodeJS.Timeout;
  return (...args: Parameters<T>) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}

export function exportToCSV<T extends Record<string, unknown>>(data: T[], filename: string) {
  if (!data.length) return;
  const headers = Object.keys(data[0]);
  const csvContent = [
    headers.join(','),
    ...data.map((row) =>
      headers.map((h) => {
        const val = row[h];
        const str = val === null || val === undefined ? '' : String(val);
        return str.includes(',') ? `"${str}"` : str;
      }).join(',')
    ),
  ].join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${filename}_${new Date().toISOString().split('T')[0]}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

export function filterBySearch<T extends Record<string, unknown>>(
  items: T[],
  search: string,
  fields: (keyof T)[]
): T[] {
  if (!search.trim()) return items;
  const q = search.toLowerCase();
  return items.filter((item) =>
    fields.some((f) => String(item[f] ?? '').toLowerCase().includes(q))
  );
}

export function paginate<T>(items: T[], page: number, pageSize: number): T[] {
  const start = (page - 1) * pageSize;
  return items.slice(start, start + pageSize);
}

export function sortItems<T extends Record<string, unknown>>(
  items: T[],
  key: keyof T,
  direction: 'asc' | 'desc'
): T[] {
  return [...items].sort((a, b) => {
    const aVal = a[key];
    const bVal = b[key];
    if (aVal === bVal) return 0;
    if (aVal === null || aVal === undefined) return 1;
    if (bVal === null || bVal === undefined) return -1;
    const comparison = aVal < bVal ? -1 : 1;
    return direction === 'asc' ? comparison : -comparison;
  });
}

export function calculateProviderEarnings(amount: number, commission: number): number {
  return amount * (1 - commission);
}

export function getInitials(name: string): string {
  return name
    .split(' ')
    .slice(0, 2)
    .map((n) => n[0]?.toUpperCase() ?? '')
    .join('');
}

export function isWithinDateRange(
  date: Date | string,
  from?: string,
  to?: string
): boolean {
  if (!from && !to) return true;
  const d = typeof date === 'string' ? new Date(date) : date;
  if (from && d < new Date(from)) return false;
  if (to && d > new Date(to + 'T23:59:59')) return false;
  return true;
}

// Mock data generators
export function generateMockUsers(count: number): User[] {
  const names = ['Amit Kumar', 'Priya Sharma', 'Rahul Singh', 'Anjali Patel', 'Vikram Reddy',
    'Sunita Verma', 'Mohan Das', 'Kavitha Nair', 'Suresh Iyer', 'Deepa Joshi'];
  const statuses: User['status'][] = ['active', 'active', 'active', 'inactive', 'suspended'];
  return Array.from({ length: count }, (_, i) => ({
    id: `user_${i + 1}`,
    name: names[i % names.length],
    email: `${names[i % names.length].toLowerCase().replace(' ', '.')}${i}@example.com`,
    phone: `98765${String(43210 + i).padStart(5, '0')}`,
    role: 'customer' as const,
    status: statuses[i % statuses.length],
    createdAt: new Date(Date.now() - Math.random() * 90 * 24 * 60 * 60 * 1000).toISOString(),
    lastActive: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
    bookingCount: Math.floor(Math.random() * 20),
    totalSpent: Math.floor(Math.random() * 15000),
    rating: parseFloat((3.5 + Math.random() * 1.5).toFixed(1)),
    city: ['Mumbai', 'Delhi', 'Bangalore', 'Pune', 'Hyderabad'][i % 5],
  }));
}

export function generateMockProviders(count: number): Provider[] {
  const names = ['Ramesh Electricals', 'Shyam Plumbing', 'Geeta Cleaning', 'Prakash AC Services',
    'Lalitha Beauty', 'Ganesh Carpentry', 'Meena Pest Control', 'Arjun Painting'];
  const services = ['electrical', 'plumbing', 'cleaning', 'ac_service', 'beauty', 'carpentry', 'pest_control', 'painting'];
  const cities = ['Mumbai', 'Delhi', 'Bangalore', 'Pune', 'Hyderabad'];
  return Array.from({ length: count }, (_, i) => ({
    id: `prov_${i + 1}`,
    name: names[i % names.length],
    email: `${names[i % names.length].toLowerCase().replace(' ', '.')}${i}@example.com`,
    phone: `87654${String(32109 + i).padStart(5, '0')}`,
    role: 'provider' as const,
    status: (['active', 'active', 'pending', 'suspended'][i % 4]) as User['status'],
    createdAt: new Date(Date.now() - Math.random() * 180 * 24 * 60 * 60 * 1000).toISOString(),
    lastActive: new Date(Date.now() - Math.random() * 3 * 24 * 60 * 60 * 1000).toISOString(),
    bookingCount: Math.floor(Math.random() * 50),
    totalSpent: 0,
    rating: parseFloat((3.8 + Math.random() * 1.2).toFixed(1)),
    serviceType: services[i % services.length],
    city: cities[i % cities.length],
    completedBookings: Math.floor(Math.random() * 100),
    earnings: Math.floor(Math.random() * 50000),
    avgRating: parseFloat((3.8 + Math.random() * 1.2).toFixed(1)),
    reviewCount: Math.floor(Math.random() * 80),
    isVerified: i % 4 !== 2,
    responseTime: Math.floor(10 + Math.random() * 50),
  }));
}

export function generateMockBookings(count: number): Booking[] {
  const services = ['plumbing', 'electrical', 'cleaning', 'carpentry', 'painting', 'ac_service'];
  const statuses: Booking['status'][] = ['pending', 'confirmed', 'in_progress', 'completed', 'cancelled', 'disputed'];
  const customers = ['Amit Kumar', 'Priya Sharma', 'Rahul Singh', 'Anjali Patel', 'Vikram Reddy'];
  const providers = ['Ramesh Electricals', 'Shyam Plumbing', 'Geeta Cleaning', 'Prakash AC', 'Ganesh Carpentry'];
  return Array.from({ length: count }, (_, i) => {
    const status = statuses[i % statuses.length];
    return {
      id: `book_${i + 1}`,
      customerId: `user_${(i % 5) + 1}`,
      customerName: customers[i % customers.length],
      customerPhone: `98765${String(43210 + i).padStart(5, '0')}`,
      providerId: `prov_${(i % 5) + 1}`,
      providerName: providers[i % providers.length],
      serviceType: services[i % services.length],
      amount: Math.floor(500 + Math.random() * 4500),
      status,
      createdAt: new Date(Date.now() - Math.random() * 60 * 24 * 60 * 60 * 1000).toISOString(),
      scheduledAt: new Date(Date.now() + (Math.random() - 0.5) * 14 * 24 * 60 * 60 * 1000).toISOString(),
      rating: status === 'completed' ? parseFloat((3 + Math.random() * 2).toFixed(1)) : undefined,
    };
  });
}

export function generateMockPayments(count: number): Payment[] {
  const methods: Payment['method'][] = ['upi', 'card', 'netbanking', 'wallet', 'cash'];
  const statuses: Payment['status'][] = ['completed', 'completed', 'pending', 'failed', 'refunded'];
  const customers = ['Amit Kumar', 'Priya Sharma', 'Rahul Singh', 'Anjali Patel', 'Vikram Reddy'];
  return Array.from({ length: count }, (_, i) => {
    const amount = Math.floor(500 + Math.random() * 4500);
    return {
      id: `pay_${i + 1}`,
      bookingId: `book_${i + 1}`,
      customerId: `user_${(i % 5) + 1}`,
      customerName: customers[i % customers.length],
      amount,
      method: methods[i % methods.length],
      status: statuses[i % statuses.length],
      createdAt: new Date(Date.now() - Math.random() * 60 * 24 * 60 * 60 * 1000).toISOString(),
      providerPayout: Math.floor(amount * 0.85),
      platformFee: Math.floor(amount * 0.15),
      transactionId: `TXN${Date.now()}${i}`,
    };
  });
}
