const admin = require("firebase-admin");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");

admin.initializeApp();

// ─── Utility: Haversine distance ────────────────────────────────────────────

/**
 * Returns the great-circle distance in kilometres between two coordinates.
 */
function haversineKm(lat1, lng1, lat2, lng2) {
  const R = 6371;
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2);
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function toRad(deg) {
  return deg * (Math.PI / 180);
}

// ─── Utility: Rate limiting ──────────────────────────────────────────────────

/**
 * Returns true if the action is allowed under the given rate limit.
 * @param {string} key   Unique key for this action (e.g. "payment_uid123")
 * @param {number} maxRequests  Maximum allowed requests in the window
 * @param {number} windowSeconds  Duration of the rate-limit window in seconds
 */
async function checkRateLimit(key, maxRequests, windowSeconds) {
  const db = admin.firestore();
  const ref = db.collection("rate_limits").doc(key);
  const now = Date.now();
  const windowMs = windowSeconds * 1000;

  return db.runTransaction(async (tx) => {
    const doc = await tx.get(ref);
    if (!doc.exists) {
      tx.set(ref, { count: 1, windowStart: now, expiresAt: now + windowMs });
      return true;
    }
    const data = doc.data();
    if (now > data.expiresAt) {
      tx.set(ref, { count: 1, windowStart: now, expiresAt: now + windowMs });
      return true;
    }
    if (data.count >= maxRequests) return false;
    tx.update(ref, { count: data.count + 1 });
    return true;
  });
}

// ─── Utility: Human-readable status ─────────────────────────────────────────

function toHumanStatus(status) {
  switch (status) {
    case "pending":     return "Waiting for provider acceptance";
    case "accepted":    return "Provider accepted your request";
    case "on_the_way":  return "Provider is on the way";
    case "arrived":     return "Provider has arrived";
    case "completed":   return "Service completed";
    case "rejected":    return "Provider rejected your request";
    case "cancelled":   return "Booking cancelled";
    default:            return status;
  }
}

// ─── Utility: Update bookings_lite ──────────────────────────────────────────

async function syncBookingsLite(bookingId, bookingData) {
  const liteData = {
    bookingId,
    userId:        bookingData.userId         || "",
    providerId:    bookingData.providerId     || "",
    serviceName:   bookingData.serviceName    || "",
    status:        bookingData.status         || "pending",
    amount:        bookingData.amount         || 0,
    address:       bookingData.address        || "",
    scheduledDate: bookingData.scheduledDate  || "",
    scheduledTime: bookingData.scheduledTime  || "",
    timestamp:     bookingData.timestamp      || Date.now()
  };
  await admin.firestore().collection("bookings_lite").doc(bookingId).set(liteData, { merge: true });
}

// ─── Smart provider matching ─────────────────────────────────────────────────

/**
 * Finds the best available provider for a given service type.
 * Score = 0.6 * (1 / (distanceKm + 1)) + 0.4 * (rating / 5)
 * When booking coordinates are supplied the real Haversine distance is used;
 * otherwise a neutral placeholder of 50 km is applied.
 * Returns the provider document snapshot or null.
 */
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
    // Use real coordinates when available; fall back to 50 km neutral distance.
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

// ─── onBookingCreated ────────────────────────────────────────────────────────

exports.onBookingCreated = onDocumentCreated("bookings/{bookingId}", async (event) => {
  const booking = event.data?.data();
  if (!booking) return;

  const bookingId = event.params.bookingId;
  const serviceType = booking.serviceType || booking.serviceName || "general";

  // Create the lightweight list-view document.
  await syncBookingsLite(bookingId, booking);

  // Attempt smart provider assignment.
  const providerDoc = await findBestProvider(
    serviceType,
    [],
    booking.customerLat ?? null,
    booking.customerLng ?? null
  );
  if (providerDoc) {
    const provider = providerDoc.data();
    await admin.firestore().collection("bookings").doc(bookingId).update({
      status: "pending",
      assignedProviderId: providerDoc.id,
      assignedProviderName: provider.name || "",
      assignedAt: Date.now()
    });

    // Notify the matched provider via FCM.
    const tokenSnapshot = await admin.firestore()
      .collection("users").doc(providerDoc.id).collection("devices").get();
    const tokens = tokenSnapshot.docs
      .map((d) => d.get("token"))
      .filter((t) => typeof t === "string" && t.length > 0);

    if (tokens.length > 0) {
      await admin.messaging().sendEachForMulticast({
        tokens,
        notification: {
          title: "New booking request",
          body: `${booking.serviceName || "New service"} booking is waiting for your acceptance`
        },
        data: { bookingId, type: "booking_created" }
      });
    }
  } else {
    // Fallback: broadcast to all providers topic.
    await admin.messaging().send({
      topic: "providers_all",
      notification: {
        title: "New booking request",
        body: `${booking.serviceName || "New service"} booking is waiting for acceptance`
      },
      data: { bookingId, type: "booking_created" }
    });
  }
});

// ─── onBookingStatusChanged ──────────────────────────────────────────────────

/**
 * Valid state-machine transitions. Key = from, Value = allowed "to" states.
 */
const VALID_TRANSITIONS = {
  pending:     ["accepted", "rejected", "cancelled"],
  accepted:    ["on_the_way", "cancelled"],
  on_the_way:  ["arrived", "cancelled"],
  arrived:     ["completed", "cancelled"],
  completed:   [],
  rejected:    [],
  cancelled:   []
};

exports.onBookingStatusChanged = onDocumentUpdated("bookings/{bookingId}", async (event) => {
  const before = event.data?.before?.data();
  const after  = event.data?.after?.data();
  if (!before || !after) return;

  const bookingId = event.params.bookingId;
  const beforeStatus = before.status || "";
  const afterStatus  = after.status  || "";

  if (beforeStatus === afterStatus) return;

  // Sync bookings_lite on any update.
  await syncBookingsLite(bookingId, after);

  // Validate state-machine transition (log only — enforcement is in Cloud Functions callable).
  const allowed = VALID_TRANSITIONS[beforeStatus] || [];
  if (!allowed.includes(afterStatus)) {
    console.warn(`Invalid transition ${beforeStatus} → ${afterStatus} for booking ${bookingId}`);
  }

  const userId = after.userId;
  if (!userId) return;

  const serviceName = after.serviceName || "Service";
  const title = "Booking update";
  const body  = `${serviceName}: ${toHumanStatus(afterStatus)}`;

  // Notify customer.
  await sendNotificationToUser(userId, title, body, {
    bookingId,
    status: afterStatus,
    type: "booking_status_changed"
  });

  // If completed, create a payment order placeholder.
  if (afterStatus === "completed" && after.paymentStatus === "pending") {
    await admin.firestore().collection("payment_orders").add({
      bookingId,
      userId,
      amount: after.amount || 0,
      serviceName,
      status: "awaiting_payment",
      createdAt: Date.now()
    });
  }
});

// ─── acceptBooking (callable) ────────────────────────────────────────────────

exports.acceptBooking = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Must be signed in.");

  const { bookingId } = request.data;
  if (!bookingId || typeof bookingId !== "string") {
    throw new HttpsError("invalid-argument", "bookingId is required.");
  }

  const bookingRef = admin.firestore().collection("bookings").doc(bookingId);

  await admin.firestore().runTransaction(async (tx) => {
    const snapshot = await tx.get(bookingRef);
    if (!snapshot.exists) throw new HttpsError("not-found", "Booking not found.");

    const booking = snapshot.data();
    if (booking.status !== "pending") {
      throw new HttpsError("failed-precondition", `Booking is already ${booking.status}.`);
    }

    const providerSnapshot = await admin.firestore().collection("providers").doc(uid).get();
    const provider = providerSnapshot.exists ? providerSnapshot.data() : {};

    tx.update(bookingRef, {
      status: "accepted",
      providerId: uid,
      providerName: provider.name || "",
      providerPhone: provider.phone || "",
      providerRating: provider.rating || 0
    });
  });

  return { success: true };
});

// ─── updateBookingStatus (callable) ─────────────────────────────────────────

exports.updateBookingStatus = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Must be signed in.");

  const { bookingId, status } = request.data;
  if (!bookingId || !status) {
    throw new HttpsError("invalid-argument", "bookingId and status are required.");
  }

  const allowed = ["accepted", "on_the_way", "arrived", "completed", "cancelled", "rejected"];
  if (!allowed.includes(status)) {
    throw new HttpsError("invalid-argument", `Invalid status: ${status}`);
  }

  const bookingRef = admin.firestore().collection("bookings").doc(bookingId);
  const snapshot = await bookingRef.get();
  if (!snapshot.exists) throw new HttpsError("not-found", "Booking not found.");

  const booking = snapshot.data();
  const isAssignedProvider = booking.providerId === uid;
  const isCustomer = booking.userId === uid;

  if (!isAssignedProvider && !isCustomer) {
    throw new HttpsError("permission-denied", "Not authorised to update this booking.");
  }

  if (isCustomer && !isAssignedProvider && status !== "cancelled") {
    throw new HttpsError("permission-denied", "Customers can only cancel bookings.");
  }

  // Validate transition.
  const fromStatus = booking.status || "pending";
  const validNext = VALID_TRANSITIONS[fromStatus] || [];
  if (!validNext.includes(status)) {
    throw new HttpsError(
      "failed-precondition",
      `Cannot transition booking from "${fromStatus}" to "${status}".`
    );
  }

  await bookingRef.update({ status });
  return { success: true };
});

// ─── initiatePayment (callable) ──────────────────────────────────────────────

exports.initiatePayment = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Must be signed in.");

  const { bookingId } = request.data;
  if (!bookingId || typeof bookingId !== "string") {
    throw new HttpsError("invalid-argument", "bookingId is required.");
  }

  // Rate-limit: max 5 payment attempts per user per minute.
  const rateLimitOk = await checkRateLimit(`payment_${uid}`, 5, 60);
  if (!rateLimitOk) {
    throw new HttpsError("resource-exhausted", "Too many payment requests. Please wait a moment.");
  }

  const bookingSnap = await admin.firestore().collection("bookings").doc(bookingId).get();
  if (!bookingSnap.exists) throw new HttpsError("not-found", "Booking not found.");

  const booking = bookingSnap.data();
  if (booking.userId !== uid) {
    throw new HttpsError("permission-denied", "Not your booking.");
  }
  if (booking.paymentStatus === "paid") {
    throw new HttpsError("failed-precondition", "Booking is already paid.");
  }

  // Return the booking amount so the client can build the Razorpay order.
  // In production, create a Razorpay order here via the Razorpay API and return the order_id.
  return {
    bookingId,
    amount: booking.amount || 0,
    currency: "INR",
    serviceName: booking.serviceName || ""
  };
});

// ─── onPaymentSuccess (HTTP webhook) ────────────────────────────────────────

exports.onPaymentSuccess = onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).send("Method not allowed");
    return;
  }

  // In production, verify the Razorpay webhook signature here:
  // const expectedSignature = crypto
  //   .createHmac("sha256", process.env.RAZORPAY_WEBHOOK_SECRET)
  //   .update(JSON.stringify(req.body))
  //   .digest("hex");
  // if (expectedSignature !== req.headers["x-razorpay-signature"]) {
  //   res.status(400).send("Invalid signature");
  //   return;
  // }

  const { bookingId, paymentId, userId } = req.body;
  if (!bookingId || !paymentId || !userId) {
    res.status(400).send("Missing required fields");
    return;
  }

  await admin.firestore().runTransaction(async (tx) => {
    const bookingRef = admin.firestore().collection("bookings").doc(bookingId);
    const snap = await tx.get(bookingRef);
    if (!snap.exists) return;
    tx.update(bookingRef, { paymentStatus: "paid", transactionId: paymentId });
  });

  // Create an immutable payment record.
  await admin.firestore().collection("payments").add({
    bookingId,
    paymentId,
    userId,
    status: "paid",
    createdAt: Date.now()
  });

  await sendNotificationToUser(userId, "Payment confirmed", "Your payment was received successfully.", {
    bookingId,
    type: "payment_confirmed"
  });

  res.status(200).json({ success: true });
});

// ─── onReviewCreated ─────────────────────────────────────────────────────────

exports.onReviewCreated = onDocumentCreated("reviews/{reviewId}", async (event) => {
  const review = event.data?.data();
  if (!review?.providerId || typeof review.rating !== "number") return;

  const providerId = review.providerId;

  const reviewsSnapshot = await admin.firestore()
    .collection("reviews")
    .where("providerId", "==", providerId)
    .get();

  const ratings = reviewsSnapshot.docs
    .map((doc) => doc.get("rating"))
    .filter((r) => typeof r === "number");

  if (ratings.length === 0) return;

  const average = ratings.reduce((sum, r) => sum + r, 0) / ratings.length;

  await admin.firestore().collection("providers").doc(providerId).set(
    { rating: Math.round(average * 10) / 10, reviewCount: ratings.length },
    { merge: true }
  );
});

// ─── cleanupStaleTracking (scheduled) ────────────────────────────────────────

exports.cleanupStaleTracking = onSchedule("every 24 hours", async () => {
  const cutoff = Date.now() - 24 * 60 * 60 * 1000; // 24 hours ago
  const db = admin.database();
  const trackingRef = db.ref("tracking");

  const snapshot = await trackingRef.orderByChild("updatedAt").endAt(cutoff).get();
  if (!snapshot.exists()) {
    console.log("No stale tracking entries to clean up.");
    return;
  }

  const deletions = [];
  snapshot.forEach((child) => {
    deletions.push(child.ref.remove());
  });

  await Promise.all(deletions);
  console.log(`Deleted ${deletions.length} stale tracking entries.`);
});

// ─── cleanupExpiredNotifications (scheduled) ─────────────────────────────────

exports.cleanupExpiredNotifications = onSchedule("every 24 hours", async () => {
  const cutoff = Date.now() - 30 * 24 * 60 * 60 * 1000; // 30 days ago

  // Use a collection group query to avoid fetching all user documents.
  const oldMessages = await admin.firestore()
    .collectionGroup("messages")
    .where("timestamp", "<", cutoff)
    .limit(500)
    .get();

  if (oldMessages.empty) {
    console.log("No expired notifications to clean up.");
    return;
  }

  const batch = admin.firestore().batch();
  oldMessages.docs.forEach((d) => batch.delete(d.ref));
  await batch.commit();
  console.log(`Deleted ${oldMessages.size} expired notifications.`);
});

// ─── cleanupRateLimits (scheduled) ───────────────────────────────────────────

exports.cleanupRateLimits = onSchedule("every 60 minutes", async () => {
  const now = Date.now();
  const expired = await admin.firestore()
    .collection("rate_limits")
    .where("expiresAt", "<", now)
    .get();

  const batch = admin.firestore().batch();
  expired.docs.forEach((d) => batch.delete(d.ref));
  if (!expired.empty) await batch.commit();
  console.log(`Cleaned up ${expired.size} expired rate limit entries.`);
});

// ─── Helper: send FCM to all devices of a user ───────────────────────────────

async function sendNotificationToUser(userId, title, body, data = {}) {
  const tokensSnapshot = await admin.firestore()
    .collection("users")
    .doc(userId)
    .collection("devices")
    .get();

  const tokens = tokensSnapshot.docs
    .map((doc) => doc.get("token"))
    .filter((token) => typeof token === "string" && token.length > 0);

  if (tokens.length === 0) return;

  const message = {
    tokens,
    notification: { title, body },
    data: Object.fromEntries(
      Object.entries(data).map(([k, v]) => [k, String(v)])
    )
  };

  const response = await admin.messaging().sendEachForMulticast(message);

  // Remove stale tokens.
  const invalidErrorCodes = new Set([
    "messaging/registration-token-not-registered",
    "messaging/invalid-registration-token"
  ]);

  const cleanupPromises = [];
  response.responses.forEach((res, index) => {
    const code = res.error?.code;
    if (code && invalidErrorCodes.has(code)) {
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


