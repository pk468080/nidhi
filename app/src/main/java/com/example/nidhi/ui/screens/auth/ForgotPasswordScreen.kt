package com.example.nidhi.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.nidhi.ui.theme.AppSpacing
import com.example.nidhi.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavController) {

    val viewModel: AuthViewModel = viewModel()

    var email by remember { mutableStateOf("") }
    var resetSent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AppSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(AppSpacing.xxxLarge))

            if (resetSent) {

                /* ── Success state ──────────────────────────────────────────── */

                Icon(
                    Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    modifier = Modifier.size(AppSpacing.xxxLarge + AppSpacing.xxxLarge),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(AppSpacing.large))

                Text(
                    "Check Your Email",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(AppSpacing.medium))

                Text(
                    "We've sent a password reset link to $email. " +
                            "Please check your inbox and follow the instructions to reset your password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(AppSpacing.xxxLarge))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navController.popBackStack() }
                ) {
                    Text("Back to Login")
                }

            } else {

                /* ── Email entry state ──────────────────────────────────────── */

                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(AppSpacing.xxxLarge + AppSpacing.xxxLarge),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(AppSpacing.large))

                Text(
                    "Forgot Password?",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(AppSpacing.medium))

                Text(
                    "Enter your email address and we'll send you a link to reset your password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(AppSpacing.large))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        error = ""
                    },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    singleLine = true,
                    isError = error.isNotEmpty()
                )

                if (error.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.large))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isResettingPassword,
                    onClick = {
                        error = ""
                        if (email.isBlank()) {
                            error = "Please enter your email address"
                            return@Button
                        }
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            error = "Please enter a valid email address"
                            return@Button
                        }
                        viewModel.sendPasswordReset(email) { success, message ->
                            if (success) {
                                resetSent = true
                            } else {
                                error = message ?: "Failed to send reset email. Please try again."
                            }
                        }
                    }
                ) {
                    if (viewModel.isResettingPassword) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(AppSpacing.extraLarge),
                            strokeWidth = AppSpacing.extraSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Send Reset Link")
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.medium))

                TextButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = { navController.popBackStack() }
                ) {
                    Text("Back to Login")
                }

            }

        }

    }

}
