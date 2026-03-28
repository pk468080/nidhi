package com.example.nidhi.data.repository

import com.example.nidhi.data.model.Review
import com.example.nidhi.firebase.FirestoreCollections
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ReviewRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun submitReview(review: Review, onResult: (Boolean) -> Unit) {
        val docRef = firestore.collection(FirestoreCollections.REVIEWS).document()
        val reviewWithId = review.copy(reviewId = docRef.id)
        docRef.set(reviewWithId)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    /**
     * One-time fetch of reviews for a given provider, ordered newest-first.
     * Uses a single [get()] call instead of a continuous snapshot listener to
     * avoid unbounded Firestore read costs for data that changes infrequently.
     */
    fun getReviewsForProvider(
        providerId: String,
        onResult: (List<Review>) -> Unit
    ) {
        firestore.collection(FirestoreCollections.REVIEWS)
            .whereEqualTo("providerId", providerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.documents.mapNotNull { it.toObject(Review::class.java) })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun hasReviewForBooking(
        bookingId: String,
        customerId: String,
        onResult: (Boolean) -> Unit
    ) {
        firestore.collection(FirestoreCollections.REVIEWS)
            .whereEqualTo("bookingId", bookingId)
            .whereEqualTo("customerId", customerId)
            .get()
            .addOnSuccessListener { snapshot -> onResult(!snapshot.isEmpty) }
            .addOnFailureListener { onResult(false) }
    }
}
