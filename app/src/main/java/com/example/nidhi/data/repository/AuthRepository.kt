package com.example.nidhi.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {

        Log.d("AUTH", "Login attempt")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    Log.d("AUTH", "Login success")
                    onResult(true, null)
                } else {
                    Log.d("AUTH", "Login error: ${task.exception?.message}")
                    onResult(false, task.exception?.message)
                }

            }
    }

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {

        Log.d("AUTH", "Register attempt")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    Log.d("AUTH", "Register success")
                    onResult(true, null)
                } else {
                    Log.d("AUTH", "Register error: ${task.exception?.message}")
                    onResult(false, task.exception?.message)
                }

            }
    }

    fun getCurrentUser() = auth.currentUser

    fun logout() {
        auth.signOut()
    }
}