# Firebase Data Shape Guide

This project now supports a real customer-provider booking lifecycle using Firestore for bookings/payments and Realtime Database for live tracking.

## 1) Firestore Collections

### `bookings/{bookingId}`

```json
{
  "bookingId": "abc123",
  "userId": "customerUid",
  "serviceName": "AC Repair",
  "address": "123 Main Street, New Delhi",
  "scheduledDate": "24 Mar 2026",
  "scheduledTime": "10:30 AM",
  "amount": 299,
  "status": "pending",
  "paymentStatus": "paid",
  "providerId": "providerUid",
  "providerName": "Rahul Sharma",
  "providerPhone": "+91 99999 99999",
  "providerRating": 4.8,
  "notes": "Please call before arrival",
  "timestamp": 1710500000000
}
```

Status lifecycle:
- `pending` -> `accepted` -> `on_the_way` -> `arrived` -> `completed`
- failure states: `rejected`, `cancelled`

### `payments/{paymentId}`

```json
{
  "bookingId": "abc123",
  "userId": "customerUid",
  "serviceName": "AC Repair",
  "amount": 299,
  "method": "upi",
  "status": "paid",
  "transactionId": "TXN_123456"
}
```

### `users/{uid}`

```json
{
  "name": "User Name",
  "email": "user@example.com",
  "role": "customer"
}
```

### `users/{uid}/devices/{token}`

```json
{
  "token": "fcm_device_token",
  "platform": "android",
  "updatedAt": 1710500000000
}
```

## 2) Realtime Database

### `tracking/{bookingId}`

```json
{
  "userId": "customerUid",
  "providerId": "providerUid",
  "status": "on_the_way",
  "eta": 12,
  "providerLat": 28.622,
  "providerLng": 77.214,
  "providerName": "Rahul Sharma",
  "providerPhone": "+91 99999 99999",
  "providerRating": 4.8
}
```

## 3) Notes

- Customer app should listen to:
  - `bookings/{bookingId}` for lifecycle + provider info
  - `tracking/{bookingId}` for live map updates
- Tracking writes are restricted to the owner/provider pair using `userId` and `providerId` on each `tracking/{bookingId}` node.
- Provider app/panel writes status and tracking updates.
- Cloud Functions use booking status changes to send FCM notifications to user device tokens.

