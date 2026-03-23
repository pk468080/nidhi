const admin = require("firebase-admin");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");

admin.initializeApp();

exports.onBookingCreated = onDocumentCreated("bookings/{bookingId}", async (event) => {
  const booking = event.data?.data();
  if (!booking) return;

  const serviceName = booking.serviceName || "New service";
  const bookingId = event.params.bookingId;

  // Temporary provider targeting strategy:
  // publish to a shared topic until a dedicated provider app and role-based routing is ready.
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

