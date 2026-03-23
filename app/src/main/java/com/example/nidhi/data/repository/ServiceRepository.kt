package com.example.nidhi.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Repository for reading provider and service data from Firestore.
 * Both the customer app (nidhi) and provider app (nidhi-provider) share
 * the same Firebase project, so these collections are readable by all
 * authenticated users.
 */
class ServiceRepository {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Returns a real-time snapshot listener for the `providers` collection,
     * optionally filtered by category. Results are delivered as raw maps so
     * the caller can deserialise into its own model class.
     */
    fun observeAvailableProviders(
        category: String? = null,
        onChanged: (List<Map<String, Any>>) -> Unit
    ): ListenerRegistration {
        var query = firestore.collection("providers")
            .whereEqualTo("isAvailable", true)
        if (!category.isNullOrBlank()) {
            query = query.whereEqualTo("category", category)
        }
        return query.addSnapshotListener { snapshot, _ ->
            val providers = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            onChanged(providers)
        }
    }

    /**
     * Returns a real-time snapshot listener for the `services` collection,
     * optionally filtered by category.
     */
    fun observeServices(
        category: String? = null,
        onChanged: (List<Map<String, Any>>) -> Unit
    ): ListenerRegistration {
        var query = firestore.collection("services").limit(50)
        if (!category.isNullOrBlank()) {
            query = query.whereEqualTo("category", category)
        }
        return query.addSnapshotListener { snapshot, _ ->
            val services = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            onChanged(services)
        }
    }
}
