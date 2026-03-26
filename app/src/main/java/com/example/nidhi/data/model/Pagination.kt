package com.example.nidhi.data.model

import com.google.firebase.firestore.DocumentSnapshot

/** Encapsulates the result of a single page fetch from Firestore. */
data class BookingPageResult(
    val bookings: List<Booking>,
    val lastVisible: DocumentSnapshot?,
    val hasMore: Boolean
)

/** Holds all UI state for an infinitely-scrollable booking list. */
data class PaginationState(
    val items: List<Booking> = emptyList(),
    val lastVisible: DocumentSnapshot? = null,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false
)
