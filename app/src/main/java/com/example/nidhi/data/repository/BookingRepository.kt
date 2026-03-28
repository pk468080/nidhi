package com.example.nidhi.data.repository

import com.example.nidhi.data.model.Booking
import com.example.nidhi.data.model.BookingPageResult
import com.example.nidhi.data.model.BookingStatus
import com.example.nidhi.firebase.FirestoreCollections
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class BookingRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance()

    companion object {
        const val PAGE_SIZE = 10
        const val CUSTOMER_PAGE_SIZE = 20
        const val PROVIDER_PAGE_SIZE = 50
    }

    fun createBooking(booking: Booking, onResult: (Boolean, String?) -> Unit) {
        val docRef = firestore.collection(FirestoreCollections.BOOKINGS).document()
        val bookingWithId = booking.copy(bookingId = docRef.id)
        docRef.set(bookingWithId)
            .addOnSuccessListener { onResult(true, docRef.id) }
            .addOnFailureListener { onResult(false, null) }
    }

    /**
     * One-time paginated fetch of bookings for a user.
     * Pass [afterDocument] as the last document from the previous page for cursor-based pagination.
     * This replaces the continuous addSnapshotListener to avoid unbounded read costs.
     */
    fun getUserBookingsPaged(
        userId: String,
        afterDocument: DocumentSnapshot? = null,
        onResult: (List<Booking>, DocumentSnapshot?) -> Unit
    ) {
        var query = firestore.collection(FirestoreCollections.BOOKINGS)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE.toLong())

        if (afterDocument != null) {
            query = query.startAfter(afterDocument)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val bookings = snapshot.documents.mapNotNull { it.toObject(Booking::class.java) }
                val lastDoc = snapshot.documents.lastOrNull()
                onResult(bookings, lastDoc)
            }
            .addOnFailureListener { onResult(emptyList(), null) }
    }

    /**
     * Legacy one-time get for users who haven't migrated to pagination yet.
     * Kept for backward compatibility — replaces the old addSnapshotListener.
     */
    fun getUserBookings(userId: String, onResult: (List<Booking>) -> Unit) {
        firestore.collection(FirestoreCollections.BOOKINGS)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE.toLong())
            .get()
            .addOnSuccessListener { snapshot ->
                val bookings = snapshot.documents.mapNotNull { it.toObject(Booking::class.java) }
                onResult(bookings)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun updateBookingStatus(bookingId: String, status: BookingStatus, onResult: (Boolean) -> Unit) {
        firestore.collection(FirestoreCollections.BOOKINGS).document(bookingId)
            .update("status", status.value)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun updateBookingPaymentStatus(bookingId: String, paymentStatus: String, onResult: (Boolean) -> Unit) {
        firestore.collection(FirestoreCollections.BOOKINGS).document(bookingId)
            .update("paymentStatus", paymentStatus)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun observeBookingById(
        bookingId: String,
        onChanged: (Booking?) -> Unit
    ): ListenerRegistration {
        return firestore.collection(FirestoreCollections.BOOKINGS).document(bookingId)
            .addSnapshotListener { snapshot, _ ->
                onChanged(snapshot?.toObject(Booking::class.java))
            }
    }

    fun observeTrackingByBookingId(
        bookingId: String,
        listener: ValueEventListener
    ): ValueEventListener {
        realtimeDb.getReference("tracking/$bookingId").addValueEventListener(listener)
        return listener
    }

    fun removeTrackingObserver(bookingId: String, listener: ValueEventListener) {
        realtimeDb.getReference("tracking/$bookingId").removeEventListener(listener)
    }

    fun assignProviderAndAcceptBooking(
        bookingId: String,
        providerId: String,
        providerName: String,
        providerPhone: String,
        providerRating: Double,
        onResult: (Boolean) -> Unit
    ) {
        firestore.collection(FirestoreCollections.BOOKINGS).document(bookingId)
            .update(
                mapOf(
                    "status" to BookingStatus.ACCEPTED.value,
                    "providerId" to providerId,
                    "providerName" to providerName,
                    "providerPhone" to providerPhone,
                    "providerRating" to providerRating
                )
            )
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun cancelBooking(bookingId: String, onResult: (Boolean) -> Unit) {
        firestore.collection(FirestoreCollections.BOOKINGS).document(bookingId)
            .update("status", BookingStatus.CANCELLED.value)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun observeProviderBookings(
        providerId: String,
        onChanged: (List<Booking>) -> Unit
    ): ListenerRegistration {
        return firestore.collection(FirestoreCollections.BOOKINGS)
            .whereEqualTo("providerId", providerId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val bookings = snapshot?.documents?.mapNotNull {
                    it.toObject(Booking::class.java)
                } ?: emptyList()
                onChanged(bookings)
            }
    }

    fun observePendingBookings(onChanged: (List<Booking>) -> Unit): ListenerRegistration {
        return firestore.collection(FirestoreCollections.BOOKINGS)
            .whereEqualTo("status", BookingStatus.PENDING.value)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val bookings = snapshot?.documents?.mapNotNull {
                    it.toObject(Booking::class.java)
                } ?: emptyList()
                onChanged(bookings)
            }
    }

    /**
     * Cursor-based paginated fetch of a user's bookings from bookings_lite.
     * Fetches [pageSize]+1 documents to determine whether more pages exist.
     */
    fun getBookingsPage(
        userId: String,
        cursor: DocumentSnapshot? = null,
        pageSize: Int = CUSTOMER_PAGE_SIZE,
        onResult: (BookingPageResult) -> Unit
    ) {
        var query: Query = firestore.collection(FirestoreCollections.BOOKINGS_LITE)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit((pageSize + 1).toLong())

        if (cursor != null) {
            query = query.startAfter(cursor)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val docs = snapshot.documents
                onResult(
                    BookingPageResult(
                        bookings = docs.take(pageSize).mapNotNull { it.toObject(Booking::class.java) },
                        lastVisible = if (docs.size > pageSize) docs[pageSize - 1] else docs.lastOrNull(),
                        hasMore = docs.size > pageSize
                    )
                )
            }
            .addOnFailureListener { onResult(BookingPageResult(emptyList(), null, false)) }
    }

    /**
     * Cursor-based paginated fetch of pending bookings from bookings_lite.
     * Used by the provider panel to load pending requests in pages.
     */
    fun getPendingBookingsPage(
        cursor: DocumentSnapshot? = null,
        pageSize: Int = PROVIDER_PAGE_SIZE,
        onResult: (BookingPageResult) -> Unit
    ) {
        var query: Query = firestore.collection(FirestoreCollections.BOOKINGS_LITE)
            .whereEqualTo("status", BookingStatus.PENDING.value)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit((pageSize + 1).toLong())

        if (cursor != null) {
            query = query.startAfter(cursor)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val docs = snapshot.documents
                onResult(
                    BookingPageResult(
                        bookings = docs.take(pageSize).mapNotNull { it.toObject(Booking::class.java) },
                        lastVisible = if (docs.size > pageSize) docs[pageSize - 1] else docs.lastOrNull(),
                        hasMore = docs.size > pageSize
                    )
                )
            }
            .addOnFailureListener { onResult(BookingPageResult(emptyList(), null, false)) }
    }
}
