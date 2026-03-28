package com.example.nidhi.notifications

import android.util.Log
import com.example.nidhi.firebase.FirestoreCollections
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

private const val TAG = "PushTokenRegistrar"

object PushTokenRegistrar {

    fun registerCurrentTokenIfLoggedIn() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isBlank()) return@addOnSuccessListener
                saveToken(userId, token)
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Failed to fetch FCM token", error)
            }
    }

    fun saveToken(userId: String, token: String) {
        FirebaseFirestore.getInstance()
            .collection(FirestoreCollections.USERS)
            .document(userId)
            .collection("devices")
            .document(token)
            .set(
                mapOf(
                    "token" to token,
                    "platform" to "android",
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnFailureListener { error ->
                Log.w(TAG, "Failed to store FCM token", error)
            }
    }
}


private const val TAG = "PushTokenRegistrar"

object PushTokenRegistrar {

    fun registerCurrentTokenIfLoggedIn() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isBlank()) return@addOnSuccessListener
                saveToken(userId, token)
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Failed to fetch FCM token", error)
            }
    }

    fun saveToken(userId: String, token: String) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("devices")
            .document(token)
            .set(
                mapOf(
                    "token" to token,
                    "platform" to "android",
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnFailureListener { error ->
                Log.w(TAG, "Failed to store FCM token", error)
            }
    }
}

