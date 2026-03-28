package com.example.nidhi.firebase

/**
 * Central registry of all Firestore collection names used by the app.
 *
 * Using constants here instead of raw strings in every repository and ViewModel
 * prevents typos, makes renames safe, and serves as living documentation of the
 * project's Firestore schema at a glance.
 */
object FirestoreCollections {
    const val BOOKINGS = "bookings"
    const val BOOKINGS_LITE = "bookings_lite"
    const val USERS = "users"
    const val PROVIDERS = "providers"
    const val SERVICES = "services"
    const val PAYMENTS = "payments"
    const val REVIEWS = "reviews"
}
