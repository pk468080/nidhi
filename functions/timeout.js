const admin = require("firebase-admin");
const { onRequest } = require("firebase-functions/v2/https");

// ─── Timeout & retry configuration ──────────────────────────────────────────

const MAX_ASSIGNMENT_ATTEMPTS = 3;
const TIMEOUT_SECONDS = 300; // 5 minutes

// ─── scheduleBookingTimeout ──────────────────────────────────────────────────

/**
 * Schedules a Cloud Tasks HTTP task to fire `TIMEOUT_SECONDS` from now.
 * Gracefully skips if Cloud Tasks env vars are not configured.
 *
 * Required environment variables (set via Firebase Functions config):
 *   TASKS_PROJECT  – GCP project ID
 *   TASKS_LOCATION – region, e.g. "asia-south1"
 *   TASKS_QUEUE    – queue name, e.g. "booking-timeouts"
 *   FUNCTIONS_URL  – base URL of deployed functions,
 *                    e.g. "https://asia-south1-myproject.cloudfunctions.net"
 *
 * @param {string} bookingId
 * @param {number} attemptNumber  1-based attempt counter
 */
async function scheduleBookingTimeout(bookingId, attemptNumber) {
  const project  = process.env.TASKS_PROJECT;
  const location = process.env.TASKS_LOCATION;
  const queue    = process.env.TASKS_QUEUE;
  const baseUrl  = process.env.FUNCTIONS_URL;

  if (!project || !location || !queue || !baseUrl) {
    console.warn(
      "Cloud Tasks env vars not configured – booking timeout scheduling skipped.",
      { bookingId, attemptNumber }
    );
    return;
  }

  let CloudTasksClient;
  try {
    ({ CloudTasksClient } = require("@google-cloud/tasks"));
  } catch (err) {
    console.warn("@google-cloud/tasks not installed – timeout scheduling skipped.", err.message);
    return;
  }

  const tasksClient = new CloudTasksClient();
  const parent = tasksClient.queuePath(project, location, queue);

  const payload = JSON.stringify({ bookingId, attemptNumber });
  const task = {
    httpRequest: {
      httpMethod: "POST",
      url: `${baseUrl}/handleBookingTimeout`,
      headers: { "Content-Type": "application/json" },
      body: Buffer.from(payload).toString("base64")
    },
    scheduleTime: {
      seconds: Math.floor(Date.now() / 1000) + TIMEOUT_SECONDS
    }
  };

  await tasksClient.createTask({ parent, task });
  console.log(`Scheduled timeout for booking ${bookingId}, attempt ${attemptNumber}.`);
}

// ─── findBestProvider (local copy to avoid circular dependency) ──────────────

function haversineKm(lat1, lng1, lat2, lng2) {
  const R = 6371;
  const toRad = (d) => d * (Math.PI / 180);
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2);
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

async function findBestProvider(serviceType, excludeIds = [], customerLat = null, customerLng = null) {
  const snapshot = await admin.firestore()
    .collection("providers")
    .where("available", "==", true)
    .where("serviceTypes", "array-contains", serviceType)
    .get();

  if (snapshot.empty) return null;

  let best = null;
  let bestScore = -1;

  for (const doc of snapshot.docs) {
    if (excludeIds.includes(doc.id)) continue;
    const p = doc.data();
    const rating = typeof p.rating === "number" ? p.rating : 3.0;
    let distanceKm = 50;
    if (customerLat != null && customerLng != null && p.lat != null && p.lng != null) {
      distanceKm = haversineKm(customerLat, customerLng, p.lat, p.lng);
    }
    const score = 0.6 * (1 / (distanceKm + 1)) + 0.4 * (rating / 5);
    if (score > bestScore) {
      bestScore = score;
      best = doc;
    }
  }
  return best;
}

// ─── sendNotificationToUser (local copy to avoid circular dependency) ────────

async function sendNotificationToUser(userId, title, body, data = {}) {
  const tokensSnapshot = await admin.firestore()
    .collection("users").doc(userId).collection("devices").get();

  const tokens = tokensSnapshot.docs
    .map((doc) => doc.get("token"))
    .filter((t) => typeof t === "string" && t.length > 0);

  if (tokens.length === 0) return;

  const response = await admin.messaging().sendEachForMulticast({
    tokens,
    notification: { title, body },
    data: Object.fromEntries(Object.entries(data).map(([k, v]) => [k, String(v)]))
  });

  const invalidErrorCodes = new Set([
    "messaging/registration-token-not-registered",
    "messaging/invalid-registration-token"
  ]);

  const cleanupPromises = [];
  response.responses.forEach((res, index) => {
    if (res.error?.code && invalidErrorCodes.has(res.error.code)) {
      cleanupPromises.push(
        admin.firestore()
          .collection("users").doc(userId)
          .collection("devices").doc(tokens[index])
          .delete()
      );
    }
  });
  await Promise.all(cleanupPromises);
}

// ─── handleBookingTimeout (HTTP function) ───────────────────────────────────

exports.handleBookingTimeout = onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).json({ error: "Method not allowed" });
    return;
  }

  const { bookingId, attemptNumber } = req.body || {};
  if (!bookingId || typeof attemptNumber !== "number") {
    res.status(400).json({ error: "bookingId and attemptNumber are required." });
    return;
  }

  const db = admin.firestore();
  const bookingSnap = await db.collection("bookings").doc(bookingId).get();

  if (!bookingSnap.exists) {
    res.status(200).json({ skipped: "Booking not found." });
    return;
  }

  const bookingData = bookingSnap.data();

  // If provider already responded, nothing to do.
  if (bookingData.status !== "pending") {
    res.status(200).json({ skipped: "Booking already progressed past pending." });
    return;
  }

  const serviceType = bookingData.serviceType || bookingData.serviceName || "general";
  const rejectedProviders = bookingData.rejectedProviders || [];
  const currentProvider  = bookingData.assignedProviderId || "";

  if (attemptNumber < MAX_ASSIGNMENT_ATTEMPTS) {
    // Build exclusion list: all previously rejected plus the current non-responding provider.
    const excludeIds = currentProvider
      ? [...new Set([...rejectedProviders, currentProvider])]
      : rejectedProviders;

    const nextProviderDoc = await findBestProvider(
      serviceType,
      excludeIds,
      bookingData.customerLat ?? null,
      bookingData.customerLng ?? null
    );

    if (nextProviderDoc) {
      const nextProvider = nextProviderDoc.data();
      await db.collection("bookings").doc(bookingId).update({
        assignedProviderId:   nextProviderDoc.id,
        assignedProviderName: nextProvider.name || "",
        assignedAt:           Date.now(),
        assignmentAttempt:    attemptNumber + 1,
        ...(currentProvider
          ? { rejectedProviders: admin.firestore.FieldValue.arrayUnion(currentProvider) }
          : {})
      });

      // Notify next provider.
      const tokenSnapshot = await db
        .collection("users").doc(nextProviderDoc.id).collection("devices").get();
      const tokens = tokenSnapshot.docs
        .map((d) => d.get("token"))
        .filter((t) => typeof t === "string" && t.length > 0);
      if (tokens.length > 0) {
        await admin.messaging().sendEachForMulticast({
          tokens,
          notification: {
            title: "New booking request",
            body: `${bookingData.serviceName || "New service"} booking is waiting for your acceptance`
          },
          data: { bookingId, type: "booking_created" }
        });
      }

      // Schedule next timeout for the new provider.
      await scheduleBookingTimeout(bookingId, attemptNumber + 1);

      res.status(200).json({ success: true, attempt: attemptNumber + 1 });
      return;
    }
  }

  // All attempts exhausted — mark as unassigned and notify customer.
  await db.collection("bookings").doc(bookingId).update({
    status:        "cancelled",
    failureReason: "No provider available after maximum assignment attempts.",
    updatedAt:     Date.now()
  });

  await sendNotificationToUser(
    bookingData.userId,
    "Could not assign provider",
    "We couldn't find a provider for your booking. Please try again."
  );

  res.status(200).json({ success: true, unassigned: true });
});

exports.scheduleBookingTimeout = scheduleBookingTimeout;
