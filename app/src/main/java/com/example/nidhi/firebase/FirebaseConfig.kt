package com.example.nidhi.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

object FirebaseConfig {
    fun initializeFirebase(context: Context) {
        FirebaseApp.initializeApp(context.applicationContext) // Initialize Firebase
    }

    fun subscribeToTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)  
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("Subscribed to topic: $topic")
                } else {
                    println("Subscription to topic failed: $topic")
                }
            }
    }
}