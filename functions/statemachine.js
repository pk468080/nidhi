const admin = require("firebase-admin");
const { HttpsError } = require("firebase-functions/v2/https");

// ─── Valid state-machine transitions ────────────────────────────────────────
// Key = current status; Value = array of allowed next statuses.

const VALID_TRANSITIONS = {
  pending:    ["accepted", "rejected", "cancelled"],
  accepted:   ["on_the_way", "cancelled"],
  on_the_way: ["arrived", "cancelled"],
  arrived:    ["completed", "cancelled"],
  completed:  [],
  rejected:   [],
  cancelled:  []
};

/**
 * Throws HttpsError if the `from → to` transition is not permitted.
 * @param {string} from  Current booking status
 * @param {string} to    Requested next status
 */
function validateTransition(from, to) {
  const allowed = VALID_TRANSITIONS[from] || [];
  if (!allowed.includes(to)) {
    throw new HttpsError(
      "failed-precondition",
      `Cannot transition booking from "${from}" to "${to}".`
    );
  }
}

/**
 * Validates the transition, updates the booking document inside a transaction,
 * and appends an entry to the `statusHistory` audit array.
 *
 * @param {string} bookingId  Firestore document ID
 * @param {string} newStatus  Target status
 * @param {string} actorId    UID of the user performing the transition
 */
async function updateBookingStatus(bookingId, newStatus, actorId) {
  const db = admin.firestore();
  const bookingRef = db.collection("bookings").doc(bookingId);

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(bookingRef);
    if (!snap.exists) {
      throw new HttpsError("not-found", "Booking not found.");
    }

    const booking = snap.data();
    const currentStatus = booking.status || "pending";

    validateTransition(currentStatus, newStatus);

    tx.update(bookingRef, {
      status: newStatus,
      updatedAt: Date.now(),
      statusHistory: admin.firestore.FieldValue.arrayUnion({
        from: currentStatus,
        to: newStatus,
        timestamp: Date.now(),
        actor: actorId
      })
    });
  });
}

module.exports = { VALID_TRANSITIONS, validateTransition, updateBookingStatus };
