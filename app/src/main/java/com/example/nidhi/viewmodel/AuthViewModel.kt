package com.example.nidhi.viewmodel

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.nidhi.data.repository.AuthRepository
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    /* OTP flow state */
    var verificationId by mutableStateOf("")
        private set
    var isSendingOtp by mutableStateOf(false)
        private set
    var isVerifyingOtp by mutableStateOf(false)
        private set
    var otpError by mutableStateOf<String?>(null)
        private set
    var isOtpAutoVerified by mutableStateOf(false)
        private set

    /* Registration metadata for phone sign-up */
    var pendingDisplayName by mutableStateOf("")
        private set
    var isPhoneRegistration by mutableStateOf(false)
        private set

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        repository.login(email, password, onResult)
    }

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        repository.register(email, password, onResult)
    }

    fun logout() {
        repository.logout()
    }

    /** Call before navigating to OTP screen for phone-based registration. */
    fun preparePhoneRegistration(displayName: String) {
        pendingDisplayName = displayName
        isPhoneRegistration = true
    }

    /** Reset all OTP-related state (call when leaving auth screens). */
    fun resetOtpState() {
        verificationId = ""
        isSendingOtp = false
        isVerifyingOtp = false
        otpError = null
        isOtpAutoVerified = false
        pendingDisplayName = ""
        isPhoneRegistration = false
    }

    /**
     * Send OTP to the given phone number.
     *
     * @param activity  The current Activity (required by Firebase).
     * @param phoneNumber  Phone in E.164 format, e.g. "+919876543210".
     * @param onCodeSent  Called when SMS code has been sent successfully.
     * @param onError  Called when sending fails.
     */
    fun sendOTP(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: () -> Unit,
        onError: (String) -> Unit
    ) {
        isSendingOtp = true
        otpError = null
        isOtpAutoVerified = false

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                /* Auto-verification (instant verification or SMS auto-read). */
                isSendingOtp = false
                isVerifyingOtp = true
                repository.signInWithPhoneCredential(credential) { success, message ->
                    isVerifyingOtp = false
                    if (success) {
                        val uid = repository.getCurrentUser()?.uid
                        if (isPhoneRegistration && uid != null) {
                            repository.saveUserToFirestore(uid, pendingDisplayName, phoneNumber) { saved, saveError ->
                                if (!saved) {
                                    Log.w("AUTH", "Firestore user save failed after auto-verify: $saveError")
                                }
                            }
                        }
                        isOtpAutoVerified = true
                    } else {
                        otpError = message
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isSendingOtp = false
                val msg = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Invalid phone number"
                    is FirebaseTooManyRequestsException -> "Too many attempts. Try again later"
                    else -> e.message ?: "Failed to send OTP"
                }
                otpError = msg
                onError(msg)
            }

            override fun onCodeSent(
                newVerificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                isSendingOtp = false
                verificationId = newVerificationId
                onCodeSent()
            }
        }

        repository.sendOTP(activity, phoneNumber, callbacks)
    }

    /**
     * Verify the OTP code entered by the user.
     *
     * For phone registration, also creates a Firestore user document on success.
     *
     * @param phoneNumber  The phone number used when sending the OTP.
     * @param code  The 6-digit code the user entered.
     * @param onResult  Called with (success, errorMessage).
     */
    fun verifyOTP(
        phoneNumber: String,
        code: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        isVerifyingOtp = true
        otpError = null
        repository.verifyOTP(verificationId, code) { success, message ->
            isVerifyingOtp = false
            if (success) {
                val uid = repository.getCurrentUser()?.uid
                if (isPhoneRegistration && uid != null) {
                    repository.saveUserToFirestore(uid, pendingDisplayName, phoneNumber) { _, _ ->
                        onResult(true, null)
                    }
                } else {
                    onResult(true, null)
                }
            } else {
                otpError = message
                onResult(false, message)
            }
        }
    }
}