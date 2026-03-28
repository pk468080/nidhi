package com.example.nidhi.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nidhi.ui.theme.AppSpacing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var phone by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    // Load existing Firestore data on open
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: run {
            isLoading = false
            return@LaunchedEffect
        }
        try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                displayName = snapshot.getString("name")
                    ?.takeIf { it.isNotBlank() }
                    ?: currentUser.displayName ?: ""
                phone = snapshot.getString("phone") ?: ""
            }
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Could not load profile data")
        } finally {
            isLoading = false
        }
    }

    fun saveProfile() {
        nameError = null
        phoneError = null
        val trimmedName = displayName.trim()
        when {
            trimmedName.isBlank() -> {
                nameError = "Name cannot be empty"
                return
            }
            trimmedName.length < 2 -> {
                nameError = "Name must be at least 2 characters"
                return
            }
            trimmedName.length > 100 -> {
                nameError = "Name cannot exceed 100 characters"
                return
            }
        }
        val trimmedPhone = phone.trim()
        if (trimmedPhone.isNotEmpty()) {
            val digits = trimmedPhone.filter { it.isDigit() }
            val isValid = digits.length == 10 || (digits.length == 12 && digits.startsWith("91"))
            if (!isValid) {
                phoneError = "Enter a valid 10-digit phone number (or include country code +91)"
                return
            }
        }
        isSaving = true
        scope.launch {
            try {
                val uid = currentUser?.uid ?: return@launch

                // 1. Update Firebase Auth display name
                val profileUpdates = userProfileChangeRequest {
                    displayName = trimmedName
                }
                currentUser.updateProfile(profileUpdates).await()

                // 2. Update Firestore document (merge so existing fields are preserved)
                val updates = mutableMapOf<String, Any>("name" to trimmedName)
                if (trimmedPhone.isNotEmpty()) updates["phone"] = trimmedPhone

                firestore.collection("users").document(uid)
                    .set(updates, SetOptions.merge()).await()

                snackbarHostState.showSnackbar("Profile updated successfully")
                navController.popBackStack()

            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    e.message ?: "Failed to update profile. Please try again."
                )
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = AppSpacing.default),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { saveProfile() }) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Save",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = AppSpacing.large, vertical = AppSpacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.large)
        ) {

            // Avatar initials circle
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(88.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (displayName.firstOrNull()?.uppercaseChar()
                                ?: currentUser?.email?.firstOrNull()?.uppercaseChar()
                                ?: 'U').toString(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Text(
                text = "Personal information",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it; nameError = null },
                label = { Text("Full name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                isError = nameError != null,
                supportingText = nameError?.let { msg -> { Text(msg) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; phoneError = null },
                label = { Text("Phone number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                placeholder = { Text("+91 XXXXX XXXXX") },
                isError = phoneError != null,
                supportingText = phoneError?.let { msg -> { Text(msg) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Text(
                text = "Account",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = currentUser?.email ?: "—",
                onValueChange = {},
                label = { Text("Email address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Text(
                text = "Email cannot be changed here. Contact support if you need to update it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppSpacing.extraSmall)
            )

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            Button(
                onClick = { saveProfile() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isSaving,
                shape = MaterialTheme.shapes.medium
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save changes", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}