package com.example.nidhi.data.repository

import com.example.nidhi.firebase.FirestoreCollections
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Repository for reading provider and service data from Firestore.
 * Both the customer app (nidhi) and provider app (nidhi-provider) share
 * the same Firebase project, so these collections are readable by all
 * authenticated users.
 */
class ServiceRepository {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * One-time fetch of available providers, optionally filtered by category.
     * Uses a single [get()] call instead of a continuous snapshot listener to
     * avoid unbounded Firestore read costs for data that changes infrequently.
     */
    fun getAvailableProviders(
        category: String? = null,
        onResult: (List<Map<String, Any>>) -> Unit
    ) {
        var query = firestore.collection(FirestoreCollections.PROVIDERS)
            .whereEqualTo("isAvailable", true)
        if (!category.isNullOrBlank()) {
            query = query.whereEqualTo("category", category)
        }
        query.get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.documents.mapNotNull { it.data })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    /**
     * One-time fetch of services, optionally filtered by category.
     * Uses a single [get()] call instead of a continuous snapshot listener to
     * avoid unbounded Firestore read costs for data that changes infrequently.
     */
    fun getServices(
        category: String? = null,
        onResult: (List<Map<String, Any>>) -> Unit
    ) {
        var query = firestore.collection(FirestoreCollections.SERVICES).limit(50)
        if (!category.isNullOrBlank()) {
            query = query.whereEqualTo("category", category)
        }
        query.get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.documents.mapNotNull { it.data })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }
}
