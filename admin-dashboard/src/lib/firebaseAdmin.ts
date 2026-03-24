/**
 * Firebase Admin SDK configuration for server-side operations.
 * Only import this in Server Components or API routes.
 */

export const ADMIN_COLLECTION = 'adminUsers';
export const USERS_COLLECTION = 'users';
export const PROVIDERS_COLLECTION = 'providers';
export const BOOKINGS_COLLECTION = 'bookings';
export const PAYMENTS_COLLECTION = 'payments';
export const SERVICES_COLLECTION = 'services';
export const REVIEWS_COLLECTION = 'reviews';
export const DISPUTES_COLLECTION = 'disputes';
export const NOTIFICATIONS_COLLECTION = 'notifications';

export const FIREBASE_COLLECTIONS = {
  adminUsers: ADMIN_COLLECTION,
  users: USERS_COLLECTION,
  providers: PROVIDERS_COLLECTION,
  bookings: BOOKINGS_COLLECTION,
  payments: PAYMENTS_COLLECTION,
  services: SERVICES_COLLECTION,
  reviews: REVIEWS_COLLECTION,
  disputes: DISPUTES_COLLECTION,
  notifications: NOTIFICATIONS_COLLECTION,
} as const;
