package com.example.nidhi.ui.screens.auth

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.nidhi.navigation.Routes
import com.example.nidhi.ui.theme.AppSpacing
import com.example.nidhi.utils.formatIndianPhoneNumber
import com.example.nidhi.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(navController: NavController) {

    val viewModel: AuthViewModel = viewModel()
    val context = LocalContext.current

    var registerMethod by remember { mutableStateOf("email") }

    /* Email registration fields */
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    /* Phone registration fields */
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var error by remember { mutableStateOf("") }

    /* Sync any OTP error from ViewModel */
    val otpError = viewModel.otpError
    LaunchedEffect(otpError) {
        if (otpError != null) error = otpError
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.large)
    ) {

        Text(
            "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(AppSpacing.large))

        /* Method selector */
        Row {
            if (registerMethod == "email") {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { registerMethod = "email" }
                ) {
                    Icon(Icons.Default.Email, null)
                    Spacer(modifier = Modifier.width(AppSpacing.extraSmall + AppSpacing.extraSmall))
                    Text("Email")
                }
            } else {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        registerMethod = "email"
                        error = ""
                    }
                ) {
                    Icon(Icons.Default.Email, null)
                    Spacer(modifier = Modifier.width(AppSpacing.extraSmall + AppSpacing.extraSmall))
                    Text("Email")
                }
            }

            Spacer(modifier = Modifier.width(AppSpacing.small))

            if (registerMethod == "phone") {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { registerMethod = "phone" }
                ) {
                    Icon(Icons.Default.Phone, null)
                    Spacer(modifier = Modifier.width(AppSpacing.extraSmall + AppSpacing.extraSmall))
                    Text("Phone")
                }
            } else {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        registerMethod = "phone"
                        error = ""
                        viewModel.resetOtpState()
                    }
                ) {
                    Icon(Icons.Default.Phone, null)
                    Spacer(modifier = Modifier.width(AppSpacing.extraSmall + AppSpacing.extraSmall))
                    Text("Phone")
                }
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.xxLarge))

        if (registerMethod == "email") {

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(AppSpacing.large))

            Button(
                onClick = {
                    error = ""
                    if (email.isEmpty() || password.isEmpty()) {
                        error = "Fields cannot be empty"
                        return@Button
                    }
                    val pwError = validatePassword(password)
                    if (pwError != null) {
                        error = pwError
                        return@Button
                    }
                    viewModel.register(email, password) { success, message ->
                        if (success) {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.REGISTER) { inclusive = true }
                            }
                        } else {
                            error = message ?: "Registration failed"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Register", style = MaterialTheme.typography.labelLarge)
            }

        } else {

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, null) },
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number (+91)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(AppSpacing.large))

            Button(
                onClick = {
                    error = ""
                    if (name.trim().isEmpty()) {
                        error = "Name cannot be empty"
                        return@Button
                    }
                    val formatted = formatIndianPhoneNumber(phone)
                    if (formatted == null) {
                        error = "Enter a valid 10-digit phone number"
                        return@Button
                    }
                    viewModel.resetOtpState()
                    viewModel.preparePhoneRegistration(name.trim())
                    (context as? Activity)?.let { activity ->
                        viewModel.sendOTP(
                            activity = activity,
                            phoneNumber = formatted,
                            onCodeSent = {
                                navController.navigate(
                                    Routes.OTP_VERIFICATION + "/${Uri.encode(formatted)}"
                                )
                            },
                            onError = { msg -> error = msg }
                        )
                    }
                },
                enabled = !viewModel.isSendingOtp,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                if (viewModel.isSendingOtp) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppSpacing.extraLarge),
                        strokeWidth = AppSpacing.extraSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send OTP", style = MaterialTheme.typography.labelLarge)
                }
            }

        }

        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AppSpacing.medium))
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Returns an error message if [password] does not meet minimum strength requirements,
 * or null if the password is acceptable.
 *
 * Rules: at least 8 characters, one uppercase letter, one digit.
 */
private fun validatePassword(password: String): String? {
    if (password.length < 8) return "Password must be at least 8 characters"
    if (!password.any { it.isUpperCase() }) return "Password must contain at least one uppercase letter"
    if (!password.any { it.isDigit() }) return "Password must contain at least one digit"
    return null
}
