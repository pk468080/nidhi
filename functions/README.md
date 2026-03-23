# Cloud Functions (FCM Triggers)

This folder contains Firebase Cloud Functions for:

1. `onBookingCreated`
   - Trigger: Firestore create on `bookings/{bookingId}`
   - Action: sends notification to temporary provider topic `providers_all`

2. `onBookingStatusChanged`
   - Trigger: Firestore update on `bookings/{bookingId}`
   - Action: sends status notifications to customer device tokens at
     `users/{uid}/devices/{token}`

## Local setup

```bash
cd functions
npm install
```

## Run emulator

```bash
npm run serve
```

## Deploy functions

```bash
npm run deploy
```

## Temporary panel note

The in-app temporary provider panel is intended for transition use only.
Once dedicated provider app is available, keep these triggers and remove panel-only assumptions.

