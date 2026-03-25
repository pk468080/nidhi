package com.example.nidhi.data.repository

import android.app.Activity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.concurrent.TimeUnit

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Sign in with email/password and then load the persisted user role from Firestore
     * so downstream code can immediately use it without an extra round-trip.
     */
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {

        Log.d("AUTH", "Login attempt")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    Log.d("AUTH", "Login success")
                    // Persist the user role locally right after login so Firestore rules
                    // that rely on role are evaluated correctly on the first request.
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        ensureUserDocumentExists(uid, email) { onResult(true, null) }
                    } else {
                        onResult(true, null)
                    }
                } else {
                    Log.d("AUTH", "Login error: ${task.exception?.message}")
                    onResult(false, task.exception?.message)
                }

            }
    }

    /**
     * Create a new email/password account, send a verification e-mail, and create the
     * Firestore user document with role = "customer".
     */
    fun register(
        email: String,
        password: String,
        name: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {

        Log.d("AUTH", "Register attempt")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    Log.d("AUTH", "Register success")
                    val user = auth.currentUser
                    // Send email verification
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { verifyTask ->
                            if (!verifyTask.isSuccessful) {
                                Log.w("AUTH", "Email verification send failed: ${verifyTask.exception?.message}")
                            }
                        }
                    if (user != null) {
                        saveEmailUserToFirestore(user.uid, name, email) { saved, saveError ->
                            if (!saved) {
                                Log.w("AUTH", "Firestore user save failed on register: $saveError")
                            }
                            onResult(true, null)
                        }
                    } else {
                        onResult(true, null)
                    }
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

    /** Save a phone-auth user document; role defaults to "customer". */
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
            // merge = true so we never overwrite an existing role (e.g. "provider")
            .set(userData, SetOptions.merge())
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

    /** Save an email/password user document; role defaults to "customer". */
    fun saveEmailUserToFirestore(
        uid: String,
        name: String,
        email: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val userData = hashMapOf(
            "id" to uid,
            "name" to name,
            "email" to email,
            "phone" to "",
            "role" to "customer",
            "createdAt" to System.currentTimeMillis()
        )
        firestore.collection("users")
            .document(uid)
            .set(userData, SetOptions.merge())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AUTH", "Email user saved to Firestore")
                    onResult(true, null)
                } else {
                    Log.d("AUTH", "Firestore save error: ${task.exception?.message}")
                    onResult(false, task.exception?.message)
                }
            }
    }

    /**
     * Load the user's persisted role from Firestore.
     * Returns the role string ("customer" or "provider") via [onResult].
     */
    fun loadUserRole(uid: String, onResult: (String) -> Unit) {
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "customer"
                onResult(role)
            }
            .addOnFailureListener {
                Log.w("AUTH", "Failed to load user role: ${it.message}")
                onResult("customer")
            }
    }

    /**
     * Ensures a Firestore document exists for the user after login.
     * Uses merge so existing fields (especially "role") are preserved.
     */
    private fun ensureUserDocumentExists(uid: String, email: String, onDone: () -> Unit) {
        val ref = firestore.collection("users").document(uid)
        ref.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val data = hashMapOf(
                    "id" to uid,
                    "email" to email,
                    "role" to "customer",
                    "createdAt" to System.currentTimeMillis()
                )
                ref.set(data, SetOptions.merge()).addOnCompleteListener { onDone() }
            } else {
                onDone()
            }
        }.addOnFailureListener { onDone() }
    }

    fun getCurrentUser() = auth.currentUser

    fun logout() {
        auth.signOut()
    }
}