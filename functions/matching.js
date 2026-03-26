/**
 * Smart provider matching module.
 *
 * Implements Uber/Zomato-grade provider selection using a composite score:
 *   score = 0.6 * (1 / (distanceKm + 1)) + 0.4 * (rating / 5)
 *
 * Exported functions:
 *   haversineKm(lat1, lng1, lat2, lng2) → number
 *   calculateScore(distanceKm, rating)  → number
 *   findBestProvider(serviceType, excludeIds, customerLat, customerLng) → DocumentSnapshot | null
 */

const admin = require("firebase-admin");

/** Fallback provider rating when none is recorded. */
const DEFAULT_PROVIDER_RATING = 3.0;
/** Neutral distance (km) used when exact coordinates are unavailable. */
const NEUTRAL_DISTANCE_KM = 50;

// ─── Haversine distance ───────────────────────────────────────────────────────

/**
 * Returns the great-circle distance in kilometres between two coordinates.
 * @param {number} lat1
 * @param {number} lng1
 * @param {number} lat2
 * @param {number} lng2
 * @returns {number}
 */
function haversineKm(lat1, lng1, lat2, lng2) {
  const R = 6371;
  const toRad = (deg) => deg * (Math.PI / 180);
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2);
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// ─── Score calculation ────────────────────────────────────────────────────────

/**
 * Calculates a composite score for a provider.
 * - Distance weight: 60% (closer is better)
 * - Rating weight:   40% (higher is better, normalised to 0-1)
 *
 * @param {number} distanceKm  Distance from customer to provider in km
 * @param {number} rating      Provider rating (0-5 scale)
 * @returns {number}           Score in range (0, 1]
 */
function calculateScore(distanceKm, rating) {
  return 0.6 * (1 / (distanceKm + 1)) + 0.4 * (rating / 5);
}

// ─── Best provider lookup ─────────────────────────────────────────────────────

/**
 * Finds the best available provider for a given service type.
 *
 * Filters providers by:
 *   - available === true
 *   - serviceTypes array-contains serviceType
 *   - NOT in excludeIds (previously rejected providers)
 *
 * Sorts by composite score (distance + rating) and returns the top match.
 *
 * @param {string}        serviceType  The type of service requested
 * @param {string[]}      excludeIds   Provider UIDs to exclude (e.g. previous rejections)
 * @param {number|null}   customerLat  Customer latitude (null → use 50 km neutral distance)
 * @param {number|null}   customerLng  Customer longitude
 * @returns {Promise<FirebaseFirestore.DocumentSnapshot|null>}
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
    const rating = typeof p.rating === "number" ? p.rating : DEFAULT_PROVIDER_RATING;

    // Use real Haversine distance when coordinates are available;
    // otherwise fall back to a neutral placeholder.
    let distanceKm = NEUTRAL_DISTANCE_KM;
    if (
      customerLat != null && customerLng != null &&
      typeof p.lat === "number" && typeof p.lng === "number"
    ) {
      distanceKm = haversineKm(customerLat, customerLng, p.lat, p.lng);
    }

    const score = calculateScore(distanceKm, rating);
    if (score > bestScore) {
      bestScore = score;
      best = doc;
    }
  }

  return best;
}

module.exports = { haversineKm, calculateScore, findBestProvider };
