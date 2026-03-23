const admin = require("firebase-admin");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");

admin.initializeApp();

exports.onBookingCreated = onDocumentCreated("bookings/{bookingId}", async (event) => {
  const booking = event.data?.data();
  if (!booking) return;

  const serviceName = booking.serviceName || "New service";
  const bookingId = event.params.bookingId;

  // Notify all providers via the shared topic so any available provider can accept.
  await admin.messaging().send({
    topic: "providers_all",
    notification: {
      title: "New booking request",
      body: `${serviceName} booking is waiting for acceptance`
    },
    data: {
      bookingId,
      type: "booking_created"
    }
  });
});

exports.onBookingStatusChanged = onDocumentUpdated("bookings/{bookingId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  if (!before || !after) return;

  const beforeStatus = before.status || "";
  const afterStatus = after.status || "";
  if (beforeStatus === afterStatus) return;

  const userId = after.userId;
  if (!userId) return;

  const serviceName = after.serviceName || "Service";
  const title = "Booking update";
  const body = `${serviceName}: ${toHumanStatus(afterStatus)}`;

  const tokensSnapshot = await admin
    .firestore()
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
    data: {
      bookingId: event.params.bookingId,
      status: afterStatus,
      type: "booking_status_changed"
    }
  };

  const response = await admin.messaging().sendEachForMulticast(message);

  // Remove invalid tokens to keep token collection clean.
  const invalidErrorCodes = new Set([
    "messaging/registration-token-not-registered",
    "messaging/invalid-registration-token"
  ]);

  const cleanupPromises = [];
  response.responses.forEach((res, index) => {
    const code = res.error?.code;
    if (code && invalidErrorCodes.has(code)) {
      const token = tokens[index];
      cleanupPromises.push(
        admin
          .firestore()
          .collection("users")
          .doc(userId)
          .collection("devices")
          .doc(token)
          .delete()
      );
    }
  });

  await Promise.all(cleanupPromises);
});

/**
 * Callable function invoked by the provider app to accept a pending booking.
 * Validates that the booking is still pending before updating it.
 */
exports.acceptBooking = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Must be signed in.");

  const { bookingId } = request.data;
  if (!bookingId) throw new HttpsError("invalid-argument", "bookingId is required.");

  const bookingRef = admin.firestore().collection("bookings").doc(bookingId);
  const snapshot = await bookingRef.get();
  if (!snapshot.exists) throw new HttpsError("not-found", "Booking not found.");

  const booking = snapshot.data();
  if (booking.status !== "pending") {
    throw new HttpsError("failed-precondition", `Booking is already ${booking.status}.`);
  }

  const providerSnapshot = await admin.firestore().collection("providers").doc(uid).get();
  const provider = providerSnapshot.exists ? providerSnapshot.data() : {};

  await bookingRef.update({
    status: "accepted",
    providerId: uid,
    providerName: provider.name || "",
    providerPhone: provider.phone || "",
    providerRating: provider.rating || 0
  });

  return { success: true };
});

/**
 * Callable function to update booking status (for provider app lifecycle steps).
 * Allowed transitions: accepted → on_the_way → arrived → completed
 *                      pending/accepted/on_the_way/arrived → cancelled (by customer)
 */
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

  // Customers may only cancel.
  if (isCustomer && !isAssignedProvider && status !== "cancelled") {
    throw new HttpsError("permission-denied", "Customers can only cancel bookings.");
  }

  await bookingRef.update({ status });
  return { success: true };
});

/**
 * Triggered when a new review document is created.
 * Recalculates the provider's average rating in the `providers` collection.
 */
exports.onReviewCreated = onDocumentCreated("reviews/{reviewId}", async (event) => {
  const review = event.data?.data();
  if (!review?.providerId || typeof review.rating !== "number") return;

  const providerId = review.providerId;

  const reviewsSnapshot = await admin
    .firestore()
    .collection("reviews")
    .whereEqualTo("providerId", providerId)
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

function toHumanStatus(status) {
  switch (status) {
    case "pending":
      return "Waiting for provider acceptance";
    case "accepted":
      return "Provider accepted your request";
    case "on_the_way":
      return "Provider is on the way";
    case "arrived":
      return "Provider has arrived";
    case "completed":
      return "Service completed";
    case "rejected":
      return "Provider rejected your request";
    case "cancelled":
      return "Booking cancelled";
    default:
      return status;
  }
}

