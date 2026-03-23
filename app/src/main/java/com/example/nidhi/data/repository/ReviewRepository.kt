package com.example.nidhi.data.repository

import com.example.nidhi.data.model.Review
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ReviewRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun submitReview(review: Review, onResult: (Boolean) -> Unit) {
        val docRef = firestore.collection("reviews").document()
        val reviewWithId = review.copy(reviewId = docRef.id)
        docRef.set(reviewWithId)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun observeReviewsForProvider(
        providerId: String,
        onChanged: (List<Review>) -> Unit
    ): ListenerRegistration {
        return firestore.collection("reviews")
            .whereEqualTo("providerId", providerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val reviews = snapshot?.documents?.mapNotNull {
                    it.toObject(Review::class.java)
                } ?: emptyList()
                onChanged(reviews)
            }
    }

    fun hasReviewForBooking(
        bookingId: String,
        customerId: String,
        onResult: (Boolean) -> Unit
    ) {
        firestore.collection("reviews")
            .whereEqualTo("bookingId", bookingId)
            .whereEqualTo("customerId", customerId)
            .get()
            .addOnSuccessListener { snapshot -> onResult(!snapshot.isEmpty) }
            .addOnFailureListener { onResult(false) }
    }
}
