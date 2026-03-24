package com.example.nidhi.data.repository

import android.app.Activity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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

    fun sendOTP(
        activity: Activity,
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun signInWithPhoneCredential(
        credential: PhoneAuthCredential,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AUTH", "Phone sign-in success")
                    onResult(true, null)
                } else {
                    val message = when (val ex = task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> when (ex.errorCode) {
                            "ERROR_INVALID_VERIFICATION_CODE" -> "The OTP you entered is incorrect"
                            "ERROR_SESSION_EXPIRED" -> "OTP has expired. Request a new one"
                            else -> ex.message ?: "Verification failed"
                        }
                        else -> task.exception?.message ?: "Verification failed"
                    }
                    Log.d("AUTH", "Phone sign-in error: $message")
                    onResult(false, message)
                }
            }
    }

    fun verifyOTP(
        verificationId: String,
        code: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneCredential(credential, onResult)
    }

    fun saveUserToFirestore(
        uid: String,
        name: String,
        phone: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val userData = hashMapOf(
            "id" to uid,
            "name" to name,
            "email" to "",
            "phone" to phone,
            "role" to "customer",
            "createdAt" to System.currentTimeMillis()
        )
        firestore.collection("users")
            .document(uid)
            .set(userData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AUTH", "User saved to Firestore")
                    onResult(true, null)
                } else {
                    Log.d("AUTH", "Firestore save error: ${task.exception?.message}")
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun getCurrentUser() = auth.currentUser

    fun logout() {
        auth.signOut()
    }
}