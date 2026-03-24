package com.example.nidhi.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.nidhi.navigation.Routes
import com.example.nidhi.ui.theme.AppSpacing
import com.example.nidhi.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPVerificationScreen(
    phoneNumber: String,
    navController: NavController
) {

    val viewModel: AuthViewModel = viewModel()
    val context = LocalContext.current

    var otp by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableStateOf(60) }
    var resendKey by remember { mutableStateOf(0) }
    val isResendEnabled = timeRemaining == 0

    /* Countdown timer – restarts whenever resendKey changes (e.g. after a resend). */
    LaunchedEffect(resendKey) {
        timeRemaining = 60
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
    }

    /* Auto-verification: Firebase verified the OTP silently (e.g., auto-read SMS). */
    val isAutoVerified = viewModel.isOtpAutoVerified
    LaunchedEffect(isAutoVerified) {
        if (isAutoVerified) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Phone") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(AppSpacing.xxLarge))

            Text(
                "Enter OTP",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            Text(
                "We sent a 6-digit code to\n$phoneNumber",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppSpacing.xxLarge))

            OutlinedTextField(
                value = otp,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otp = it },
                label = { Text("6-digit OTP") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center)
            )

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            /* Resend / Timer */
            if (isResendEnabled) {
                TextButton(
                    onClick = {
                        resendKey++
                        (context as? Activity)?.let { activity ->
                            viewModel.sendOTP(
                                activity = activity,
                                phoneNumber = phoneNumber,
                                onCodeSent = { /* already on OTP screen */ },
                                onError = { /* error handled via viewModel.otpError */ }
                            )
                        }
                    }
                ) {
                    Text("Resend OTP")
                }
            } else {
                Text(
                    "Resend in ${timeRemaining}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.large))

            /* Verify button */
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = otp.length == 6 && !viewModel.isVerifyingOtp,
                onClick = {
                    viewModel.verifyOTP(phoneNumber, otp) { success, _ ->
                        if (success) {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    }
                }
            ) {
                if (viewModel.isVerifyingOtp) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppSpacing.extraLarge),
                        strokeWidth = AppSpacing.extraSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Verify OTP")
                }
            }

            viewModel.otpError?.let { error ->
                Spacer(modifier = Modifier.height(AppSpacing.medium))
                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

        }

    }

}
