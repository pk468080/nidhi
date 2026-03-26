const admin = require("firebase-admin");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { CloudTasksClient } = require("@google-cloud/tasks");
const { haversineKm, findBestProvider } = require("./matching");

admin.initializeApp();

// ─── Constants ────────────────────────────────────────────────────────────────

const MAX_ASSIGNMENT_ATTEMPTS = 3;
/** Acceptance window in seconds before timeout triggers a retry. */
const ACCEPTANCE_TIMEOUT_SECONDS = 300; // 5 minutes
/** Back-off between retry attempts in seconds. */
const RETRY_BACKOFF_SECONDS = 30;

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
    case "unassigned":  return "No provider found — please try again";
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

// ─── Utility: build deduplicated rejected-providers list ─────────────────────

/**
 * Returns a new array containing all IDs from `existing` plus `newId`,
 * with duplicates removed.
 * @param {string[]} existing
 * @param {string}   newId
 * @returns {string[]}
 */
function addToRejectedProviders(existing, newId) {
  return [...new Set([...existing, newId])];
}

// ─── Helper: assign a provider and notify ────────────────────────────────────

/**
 * Assigns the best available provider to a booking and sends an FCM notification.
 * Updates the booking document with assignment metadata and schedules a timeout
 * task so that unresponsive providers are automatically retried.
 *
 * @param {string}   bookingId
 * @param {object}   booking         Current booking data
 * @param {string[]} rejectedIds     Provider IDs to exclude from this attempt
 * @param {number}   attemptNum      1-based attempt counter
 */
async function assignProvider(bookingId, booking, rejectedIds, attemptNum) {
  const serviceType = booking.serviceType || booking.serviceName || "general";

  const providerDoc = await findBestProvider(
    serviceType,
    rejectedIds,
    booking.customerLat ?? null,
    booking.customerLng ?? null
  );

  const bookingRef = admin.firestore().collection("bookings").doc(bookingId);
  const now = Date.now();

  if (!providerDoc) {
    // No provider found — mark as unassigned and notify customer.
    await bookingRef.update({
      status: "unassigned",
      assignmentAttempt: attemptNum,
      updatedAt: now
    });
    await syncBookingsLite(bookingId, { ...booking, status: "unassigned" });
    await sendNotificationToUser(
      booking.userId,
      "No provider available",
      "We could not find an available provider for your booking. Please try again later.",
      { bookingId, type: "no_provider_available" }
    );
    return;
  }

  const provider = providerDoc.data();
  const historyEntry = {
    attemptNum,
    assignedProvider: providerDoc.id,
    assignedAt: now,
    status: "pending"
  };

  await bookingRef.update({
    status: "pending",
    assignedProviderId: providerDoc.id,
    assignedProviderName: provider.name || "",
    assignedAt: now,
    assignmentAttempt: attemptNum,
    rejectedProviders: rejectedIds,
    assignmentHistory: admin.firestore.FieldValue.arrayUnion(historyEntry),
    updatedAt: now
  });
  await syncBookingsLite(bookingId, { ...booking, status: "pending", assignedProviderId: providerDoc.id });

  // Notify the matched provider.
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
      data: { bookingId, type: "booking_created", attemptNum: String(attemptNum) }
    });
  }

  // Schedule a timeout task — if provider does not respond within the acceptance
  // window, the task handler will trigger the next retry attempt.
  await scheduleTimeoutTask(bookingId, attemptNum);
}

// ─── Helper: schedule a Cloud Tasks timeout ──────────────────────────────────

/**
 * Enqueues a Cloud Task that fires after ACCEPTANCE_TIMEOUT_SECONDS.
 * The task calls the `handleAssignmentTimeout` HTTP function.
 * Requires GOOGLE_CLOUD_PROJECT and CLOUD_TASKS_QUEUE env vars to be set,
 * or falls back gracefully when running in the local emulator.
 *
 * @param {string} bookingId
 * @param {number} attemptNum
 */
async function scheduleTimeoutTask(bookingId, attemptNum) {
  const projectId = process.env.GOOGLE_CLOUD_PROJECT;
  const queueName = process.env.CLOUD_TASKS_QUEUE || "provider-timeout";
  const location = process.env.CLOUD_TASKS_LOCATION || "us-central1";
  const functionUrl = process.env.HANDLE_TIMEOUT_URL;

  if (!projectId || !functionUrl) {
    // Running locally / emulator — skip Cloud Tasks scheduling.
    console.log(`[scheduleTimeoutTask] Skipping: env vars not set for booking ${bookingId}`);
    return;
  }

  const client = new CloudTasksClient();
  const parent = client.queuePath(projectId, location, queueName);

  const payload = JSON.stringify({ bookingId, attemptNum });
  const scheduleTime = Math.floor(Date.now() / 1000) + ACCEPTANCE_TIMEOUT_SECONDS;

  await client.createTask({
    parent,
    task: {
      httpRequest: {
        httpMethod: "POST",
        url: functionUrl,
        headers: { "Content-Type": "application/json" },
        body: payload
      },
      scheduleTime: { seconds: scheduleTime }
    }
  });
}

/**
 * Enqueues a Cloud Task with a backoff delay to retry provider assignment.
 * The task calls the `handleRetryAssignment` HTTP function.
 * Gracefully skips when running in the local emulator.
 *
 * @param {string}   bookingId
 * @param {number}   nextAttempt     The attempt number to use in the retry
 * @param {string[]} rejectedIds     Provider IDs to exclude from the retry
 * @param {number}   delaySeconds    How many seconds from now to schedule the task
 */
async function scheduleRetryTask(bookingId, nextAttempt, rejectedIds, delaySeconds) {
  const projectId = process.env.GOOGLE_CLOUD_PROJECT;
  const queueName = process.env.CLOUD_TASKS_QUEUE || "provider-timeout";
  const location = process.env.CLOUD_TASKS_LOCATION || "us-central1";
  const functionUrl = process.env.HANDLE_RETRY_URL;

  if (!projectId || !functionUrl) {
    // Running locally / emulator — fall back to immediate in-process retry.
    console.log(`[scheduleRetryTask] Skipping Cloud Tasks: env vars not set for booking ${bookingId}`);
    const bookingSnap = await admin.firestore().collection("bookings").doc(bookingId).get();
    if (bookingSnap.exists) {
      await assignProvider(bookingId, bookingSnap.data(), rejectedIds, nextAttempt);
    }
    return;
  }

  const client = new CloudTasksClient();
  const parent = client.queuePath(projectId, location, queueName);

  const payload = JSON.stringify({ bookingId, attemptNum: nextAttempt, rejectedProviders: rejectedIds });
  const scheduleTime = Math.floor(Date.now() / 1000) + delaySeconds;

  await client.createTask({
    parent,
    task: {
      httpRequest: {
        httpMethod: "POST",
        url: functionUrl,
        headers: { "Content-Type": "application/json" },
        body: payload
      },
      scheduleTime: { seconds: scheduleTime }
    }
  });
}

// ─── onBookingCreated ────────────────────────────────────────────────────────

exports.onBookingCreated = onDocumentCreated("bookings/{bookingId}", async (event) => {
  const booking = event.data?.data();
  if (!booking) return;

  const bookingId = event.params.bookingId;

  // Create the lightweight list-view document.
  await syncBookingsLite(bookingId, booking);

  // Initialise assignment tracking fields.
  await admin.firestore().collection("bookings").doc(bookingId).update({
    assignmentAttempt: 0,
    rejectedProviders: [],
    assignmentHistory: []
  });

  // First provider assignment attempt.
  await assignProvider(bookingId, booking, [], 1);
});

// ─── onBookingStatusChanged ──────────────────────────────────────────────────

/**
 * Valid state-machine transitions. Key = from, Value = allowed "to" states.
 */
const VALID_TRANSITIONS = {
  pending:     ["accepted", "rejected", "cancelled", "unassigned"],
  accepted:    ["on_the_way", "cancelled"],
  on_the_way:  ["arrived", "cancelled"],
  arrived:     ["completed", "cancelled"],
  completed:   [],
  rejected:    [],
  cancelled:   [],
  unassigned:  ["cancelled"]
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

// ─── rejectBooking (callable) ─────────────────────────────────────────────────

/**
 * Called by a provider to reject an assigned booking.
 * Records the rejection and triggers the next provider assignment attempt.
 * After MAX_ASSIGNMENT_ATTEMPTS failures the booking is marked "unassigned"
 * and the customer is notified.
 */
exports.rejectBooking = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Must be signed in.");

  const { bookingId } = request.data;
  if (!bookingId || typeof bookingId !== "string") {
    throw new HttpsError("invalid-argument", "bookingId is required.");
  }

  const bookingRef = admin.firestore().collection("bookings").doc(bookingId);
  const snapshot = await bookingRef.get();
  if (!snapshot.exists) throw new HttpsError("not-found", "Booking not found.");

  const booking = snapshot.data();

  // Only the currently assigned provider can reject.
  if (booking.assignedProviderId !== uid) {
    throw new HttpsError("permission-denied", "You are not the assigned provider for this booking.");
  }

  if (booking.status !== "pending") {
    throw new HttpsError("failed-precondition", `Booking cannot be rejected in status "${booking.status}".`);
  }

  const now = Date.now();
  const currentAttempt = booking.assignmentAttempt || 1;
  const rejectedProviders = Array.isArray(booking.rejectedProviders) ? booking.rejectedProviders : [];
  const newRejectedProviders = addToRejectedProviders(rejectedProviders, uid);

  // Update the last history entry to reflect rejection.
  await bookingRef.update({
    assignmentHistory: admin.firestore.FieldValue.arrayUnion({
      attemptNum: currentAttempt,
      assignedProvider: uid,
      assignedAt: booking.assignedAt || now,
      rejectedAt: now,
      status: "rejected"
    }),
    rejectedProviders: newRejectedProviders,
    assignedProviderId: admin.firestore.FieldValue.delete(),
    assignedProviderName: admin.firestore.FieldValue.delete(),
    updatedAt: now
  });

  const nextAttempt = currentAttempt + 1;

  if (nextAttempt > MAX_ASSIGNMENT_ATTEMPTS) {
    // All attempts exhausted — mark as unassigned and notify customer.
    await bookingRef.update({ status: "unassigned", assignmentAttempt: nextAttempt, updatedAt: now });
    await syncBookingsLite(bookingId, { ...booking, status: "unassigned" });
    await sendNotificationToUser(
      booking.userId,
      "No provider available",
      "We could not find an available provider after multiple attempts. Please try again later.",
      { bookingId, type: "no_provider_available" }
    );
    return { success: true, status: "unassigned" };
  }

  // Schedule the next assignment attempt via Cloud Tasks with a backoff delay.
  // This avoids blocking the function instance during the retry wait.
  await scheduleRetryTask(bookingId, nextAttempt, newRejectedProviders, RETRY_BACKOFF_SECONDS);

  return { success: true, status: "retrying", attempt: nextAttempt };
});

// ─── handleAssignmentTimeout (HTTP, called by Cloud Tasks) ───────────────────

/**
 * Triggered by Cloud Tasks when a provider has not responded within the
 * acceptance window (ACCEPTANCE_TIMEOUT_SECONDS).
 * If the booking is still "pending" with the same attempt number,
 * the current provider is evicted and the next attempt is started.
 */
exports.handleAssignmentTimeout = onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).send("Method not allowed");
    return;
  }

  const { bookingId, attemptNum } = req.body;
  if (!bookingId || typeof attemptNum !== "number" ||
      !Number.isInteger(attemptNum) || attemptNum < 1 || attemptNum > MAX_ASSIGNMENT_ATTEMPTS) {
    res.status(400).send("Invalid fields: bookingId (string) and attemptNum (1–MAX_ASSIGNMENT_ATTEMPTS integer) are required.");
    return;
  }

  const bookingRef = admin.firestore().collection("bookings").doc(bookingId);
  const snapshot = await bookingRef.get();
  if (!snapshot.exists) {
    res.status(200).json({ skipped: true, reason: "booking not found" });
    return;
  }

  const booking = snapshot.data();

  // Skip if the booking is no longer in a pending-assignment state or if
  // a later attempt has already replaced this one.
  if (booking.status !== "pending" || booking.assignmentAttempt !== attemptNum) {
    res.status(200).json({ skipped: true, reason: "stale timeout" });
    return;
  }

  const now = Date.now();
  const timedOutProvider = booking.assignedProviderId;
  const rejectedProviders = Array.isArray(booking.rejectedProviders) ? booking.rejectedProviders : [];
  const newRejectedProviders = timedOutProvider
    ? addToRejectedProviders(rejectedProviders, timedOutProvider)
    : rejectedProviders;

  // Record the timeout in assignment history.
  if (timedOutProvider) {
    await bookingRef.update({
      assignmentHistory: admin.firestore.FieldValue.arrayUnion({
        attemptNum,
        assignedProvider: timedOutProvider,
        assignedAt: booking.assignedAt || now,
        timedOutAt: now,
        status: "timed_out"
      }),
      rejectedProviders: newRejectedProviders,
      assignedProviderId: admin.firestore.FieldValue.delete(),
      assignedProviderName: admin.firestore.FieldValue.delete(),
      updatedAt: now
    });
  }

  const nextAttempt = attemptNum + 1;

  if (nextAttempt > MAX_ASSIGNMENT_ATTEMPTS) {
    await bookingRef.update({ status: "unassigned", assignmentAttempt: nextAttempt, updatedAt: now });
    await syncBookingsLite(bookingId, { ...booking, status: "unassigned" });
    await sendNotificationToUser(
      booking.userId,
      "No provider available",
      "We could not find an available provider after multiple attempts. Please try again later.",
      { bookingId, type: "no_provider_available" }
    );
    res.status(200).json({ success: true, status: "unassigned" });
    return;
  }

  const updatedBooking = (await bookingRef.get()).data();
  await assignProvider(bookingId, updatedBooking, newRejectedProviders, nextAttempt);

  res.status(200).json({ success: true, status: "retrying", attempt: nextAttempt });
});

// ─── handleRetryAssignment (HTTP, called by Cloud Tasks) ─────────────────────

/**
 * Triggered by Cloud Tasks (after a rejection backoff) to attempt the next
 * provider assignment. Validates that the booking is still awaiting assignment
 * before proceeding to avoid stale retries.
 */
exports.handleRetryAssignment = onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).send("Method not allowed");
    return;
  }

  const { bookingId, attemptNum, rejectedProviders: rejectedFromTask } = req.body;
  if (!bookingId || typeof attemptNum !== "number" ||
      !Number.isInteger(attemptNum) || attemptNum < 1 || attemptNum > MAX_ASSIGNMENT_ATTEMPTS) {
    res.status(400).send("Invalid fields: bookingId (string) and attemptNum (1–MAX_ASSIGNMENT_ATTEMPTS integer) are required.");
    return;
  }

  const bookingRef = admin.firestore().collection("bookings").doc(bookingId);
  const snapshot = await bookingRef.get();
  if (!snapshot.exists) {
    res.status(200).json({ skipped: true, reason: "booking not found" });
    return;
  }

  const booking = snapshot.data();

  // Guard against stale tasks: only proceed if still searching for a provider.
  if (booking.status !== "pending" && booking.status !== "searching") {
    res.status(200).json({ skipped: true, reason: `booking already in status "${booking.status}"` });
    return;
  }

  const rejectedIds = Array.isArray(rejectedFromTask) ? rejectedFromTask :
    (Array.isArray(booking.rejectedProviders) ? booking.rejectedProviders : []);

  await assignProvider(bookingId, booking, rejectedIds, attemptNum);

  res.status(200).json({ success: true, attempt: attemptNum });
});

// ─── updateBookingStatus (callable) ─────────────────────────────────────────

exports.updateBookingStatus = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Must be signed in.");

  const { bookingId, status } = request.data;
  if (!bookingId || !status) {
    throw new HttpsError("invalid-argument", "bookingId and status are required.");
  }

  const allowed = ["accepted", "on_the_way", "arrived", "completed", "cancelled", "rejected", "unassigned"];
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


