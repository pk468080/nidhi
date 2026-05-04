i build this project with the help of AI tools like ChatGPT, Claude, Github copilot

# Nidhi

Android client for an on-demand **service booking** experience: customers browse services, book with address and schedule, pay with **Razorpay**, track the provider in real time, and leave reviews. A **temporary in-app provider panel** lets providers accept or reject jobs and update status until a dedicated provider app exists.

Backend logic lives in **Firebase** (Firestore, Realtime Database, Authentication, Cloud Messaging) plus **Firebase Cloud Functions** (Node 20) for bookings, payments, notifications, and housekeeping.

## Features

- **Authentication**: Email/password (with verification flow), phone OTP, password reset; session handling with redirect on expiry.
- **Discovery & booking**: Service search and details, booking flow with location (Maps & Play Services location), ETA helpers, booking list and details.
- **Payments**: Razorpay Checkout from the Android app; server-side webhook verification and Firestore updates in Cloud Functions.
- **Live tracking**: Realtime Database path `tracking/{bookingId}` for provider/customer positions; maps-based tracking UI and offline queue utilities.
- **Push**: FCM registration, notification tap → booking details; Cloud Functions send status updates to customer devices (`users/{uid}/devices/{token}`).
- **Reviews**: Post-service reviews; Functions aggregate ratings on `providers`.
- **Provider panel**: Accept/reject, status updates, assignment flows coordinated with callable Functions.

## Tech stack

| Layer | Technologies |
|--------|----------------|
| App | Kotlin, Jetpack Compose, Material 3, Navigation Compose, ViewModels |
| Google | Maps Compose, Play Services Maps & Location, FCM |
| Firebase | Auth, Firestore, Realtime Database, Analytics, Messaging |
| Payments | Razorpay Android SDK (`checkout`) |
| Backend | Cloud Functions v2 (Firestore triggers, HTTPS callables, HTTP webhooks, scheduled jobs), optional Cloud Tasks for assignment timeouts/retries |

## Repository layout

```
app/                 # Android application module (Compose UI, repositories, ViewModels)
functions/           # Firebase Cloud Functions (see functions/README.md)
firestore.rules      # Firestore security rules
database.rules.json  # Realtime Database rules
firebase.json        # Firebase project wiring
```

Firestore collections used in app code include: `bookings`, `bookings_lite`, `users`, `providers`, `services`, `payments`, `reviews` (see `FirestoreCollections.kt`). Functions also use collections such as `idempotency_keys`, `payment_orders`, `rate_limits`, and subcollections like `users/{uid}/devices`.

## Prerequisites

- **Android Studio** (recent stable) with Android SDK **34**
- **JDK 11** (as configured in Gradle)
- A **Firebase** project with Android app `com.example.nidhi` (or change `applicationId` / Firebase config to match your package)
- **Razorpay** account (Key ID for the client; webhook secret and optional order APIs for production server flows)
- **Google Maps SDK** API key enabled for Maps (and billing as required by Google)

## Android app setup

1. Clone the repository and open the project root in Android Studio.

2. Place **`google-services.json`** in `app/` (from the Firebase console). It is required for Firebase services.

3. Create or edit **`local.properties`** in the project root (this file is normally gitignored) and add your Razorpay publishable Key ID so it is injected at build time:

   ```properties
   RAZORPAY_KEY_ID=your_razorpay_key_id_here
   ```

   If this is missing, the app still compiles; payment UI should guard against a blank key at runtime.

4. **Maps API key**: The app expects a Maps API key in the manifest (`com.google.android.geo.API_KEY`). For production, prefer restricting the key in Google Cloud Console and avoid committing unrestricted keys; you can replace the meta-data value with a build-time placeholder or flavor-specific manifest if you split environments.

5. Sync Gradle and run the **`app`** configuration on a device or emulator (**minSdk 27**).

Run tests:

```bash
./gradlew test
./gradlew connectedAndroidTest   # requires a device/emulator
```

## Firebase Cloud Functions

Functions implement booking creation (idempotent), provider matching and assignment, acceptance timeouts and retries (with **Cloud Tasks** when `GOOGLE_CLOUD_PROJECT`, queue/location, and handler URLs are configured), status updates, payment initiation rate limits, Razorpay webhook handling, review aggregation, and scheduled cleanup.

```bash
cd functions
npm install
npm run serve    # emulator: functions only
npm run deploy   # deploy functions to Firebase
```

See [`functions/README.md`](functions/README.md) for trigger overview. For full assignment timeouts in production, configure the environment variables referenced in `functions/index.js` (for example `CLOUD_TASKS_QUEUE`, `HANDLE_TIMEOUT_URL`, `HANDLE_RETRY_URL`, `RAZORPAY_WEBHOOK_SECRET`).

## Firebase project deploy

From the repo root (with Firebase CLI logged in and project selected):

```bash
firebase deploy
```

Deploy only what you need, for example:

```bash
firebase deploy --only firestore:rules,database,functions
```

## Root `package.json`

The top-level `package.json` lists a few Node dependencies (e.g. for Razorpay-related scripts or tooling). The primary Node backend for this product is under **`functions/`**.

## Security notes

- Do not commit production **Razorpay** secrets or unrestricted **Maps** keys; use CI secrets and restricted API keys where possible.
- Review **`firestore.rules`** and **`database.rules.json`** before any production deployment.

## License

This project does not include a license file in the repository. Add one (for example MIT or Apache-2.0) if you intend to open-source or distribute the code.
