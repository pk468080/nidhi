package com.example.nidhi.data.repository

import com.example.nidhi.data.model.Booking
import com.example.nidhi.data.model.BookingStatus
import com.google.firebase.firestore.FirebaseFirestore

class BookingRepository {

    private val firestore = FirebaseFirestore.getInstance()

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
}
