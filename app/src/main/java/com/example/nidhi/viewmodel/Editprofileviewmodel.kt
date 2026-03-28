package com.example.nidhi.viewmodel


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class EditProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // ── UI state ─────────────────────────────────────────────────────────────

    var displayName by mutableStateOf("")
        private set

    var phone by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var saveSuccess by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Password change sub-state
    var currentPassword by mutableStateOf("")
        private set

    var newPassword by mutableStateOf("")
        private set

    var confirmPassword by mutableStateOf("")
        private set

    var isPasswordSectionVisible by mutableStateOf(false)
        private set

    var passwordChangeSuccess by mutableStateOf(false)
        private set

    // ── Initialise from current user ──────────────────────────────────────────

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val user = auth.currentUser ?: return
        displayName = user.displayName ?: ""

        // Also pull phone from Firestore in case it was set there
        firestore.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    phone = doc.getString("phone") ?: ""
                    if (displayName.isBlank()) {
                        displayName = doc.getString("name") ?: ""
                    }
                }
            }
    }

    // ── Field updates ─────────────────────────────────────────────────────────

    fun onDisplayNameChange(value: String) { displayName = value; clearMessages() }
    fun onPhoneChange(value: String) { phone = value; clearMessages() }
    fun onCurrentPasswordChange(value: String) { currentPassword = value; clearMessages() }
    fun onNewPasswordChange(value: String) { newPassword = value; clearMessages() }
    fun onConfirmPasswordChange(value: String) { confirmPassword = value; clearMessages() }

    fun togglePasswordSection() {
        isPasswordSectionVisible = !isPasswordSectionVisible
        currentPassword = ""
        newPassword = ""
        confirmPassword = ""
        clearMessages()
    }

    // ── Save profile ──────────────────────────────────────────────────────────

    fun saveProfile() {
        val user = auth.currentUser ?: return

        if (displayName.isBlank()) {
            errorMessage = "Name cannot be empty."
            return
        }

        isLoading = true
        clearMessages()

        // 1. Update Firebase Auth display name
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName.trim())
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { authTask ->
                if (!authTask.isSuccessful) {
                    isLoading = false
                    errorMessage = authTask.exception?.message ?: "Failed to update name."
                    return@addOnCompleteListener
                }

                // 2. Update Firestore document
                val updates = mapOf(
                    "name"  to displayName.trim(),
                    "phone" to phone.trim()
                )

                firestore.collection("users").document(user.uid)
                    .set(updates, SetOptions.merge())
                    .addOnCompleteListener { firestoreTask ->
                        isLoading = false
                        if (firestoreTask.isSuccessful) {
                            saveSuccess = true
                        } else {
                            errorMessage = firestoreTask.exception?.message
                                ?: "Profile saved to Auth but failed in Firestore."
                        }
                    }
            }
    }

    // ── Change password ───────────────────────────────────────────────────────

    fun changePassword() {
        val user = auth.currentUser ?: return
        val email = user.email ?: run {
            errorMessage = "Password change is only available for email accounts."
            return
        }

        when {
            currentPassword.isBlank() -> { errorMessage = "Enter your current password."; return }
            newPassword.length < 6    -> { errorMessage = "New password must be at least 6 characters."; return }
            newPassword != confirmPassword -> { errorMessage = "Passwords do not match."; return }
        }

        isLoading = true
        clearMessages()

        // Re-authenticate first, then update password
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnCompleteListener { reAuthTask ->
                if (!reAuthTask.isSuccessful) {
                    isLoading = false
                    errorMessage = "Current password is incorrect."
                    return@addOnCompleteListener
                }

                user.updatePassword(newPassword)
                    .addOnCompleteListener { pwTask ->
                        isLoading = false
                        if (pwTask.isSuccessful) {
                            passwordChangeSuccess = true
                            isPasswordSectionVisible = false
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                        } else {
                            errorMessage = pwTask.exception?.message ?: "Failed to change password."
                        }
                    }
            }
    }

    fun resetSaveSuccess() { saveSuccess = false }
    fun resetPasswordSuccess() { passwordChangeSuccess = false }

    private fun clearMessages() {
        errorMessage = null
        saveSuccess = false
        passwordChangeSuccess = false
    }
}