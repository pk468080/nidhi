package com.example.nidhi.data.repository

import com.example.nidhi.data.model.Booking
import com.example.nidhi.data.model.BookingStatus
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class BookingRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance()

    fun createBooking(booking: Booking, onResult: (Boolean, String?) -> Unit) {
        val docRef = firestore.collection("bookings").document()
        val bookingWithId = booking.copy(bookingId = docRef.id)
        docRef.set(bookingWithId)
            .addOnSuccessListener { onResult(true, docRef.id) }
            .addOnFailureListener { onResult(false, null) }
    }

    fun getUserBookings(userId: String, onResult: (List<Booking>) -> Unit) {
        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                val bookings = snapshot?.documents?.mapNotNull {
                    it.toObject(Booking::class.java)
                } ?: emptyList()
                onResult(bookings)
            }
    }

    fun updateBookingStatus(bookingId: String, status: BookingStatus, onResult: (Boolean) -> Unit) {
        firestore.collection("bookings").document(bookingId)
            .update("status", status.value)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun updateBookingPaymentStatus(bookingId: String, paymentStatus: String, onResult: (Boolean) -> Unit) {
        firestore.collection("bookings").document(bookingId)
            .update("paymentStatus", paymentStatus)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun observeBookingById(
        bookingId: String,
        onChanged: (Booking?) -> Unit
    ): ListenerRegistration {
        return firestore.collection("bookings").document(bookingId)
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
        firestore.collection("bookings").document(bookingId)
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
}
