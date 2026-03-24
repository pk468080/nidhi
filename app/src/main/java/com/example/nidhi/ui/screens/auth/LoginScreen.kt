package com.example.nidhi.ui.screens.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.nidhi.navigation.Routes
import com.example.nidhi.ui.theme.AppSpacing
import com.example.nidhi.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {

    val viewModel: AuthViewModel = viewModel()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    var loginMethod by remember { mutableStateOf("email") }

    var showPassword by remember { mutableStateOf(false) }

    val firebaseAuth = FirebaseAuth.getInstance()

    /* Google SignIn */

    val googleSignInClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("111918879978-elsfl9lbfhfaprbrtitdehfhrt3nc7pg.apps.googleusercontent.com")
            .requestEmail()
            .build()
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {

            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {

                val account = task.getResult(ApiException::class.java)

                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->

                        if (authTask.isSuccessful) {

                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }

                        } else {

                            error = authTask.exception?.message ?: "Google login failed"

                        }

                    }

            } catch (e: Exception) {

                Log.e("GOOGLE_LOGIN", e.message ?: "Error")
                error = "Google login failed"

            }

        }

    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        /* HEADER */

        Box(

            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                )
                .padding(top = AppSpacing.xxxLarge, bottom = AppSpacing.xxxLarge),

            contentAlignment = Alignment.Center

        ) {

            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Icon(
                    Icons.Default.HomeRepairService,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(AppSpacing.xxxLarge + AppSpacing.medium)
                )

                Spacer(modifier = Modifier.height(AppSpacing.small))

                Text(
                    "Nidhi Services",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Text(
                    "Your trusted home services partner",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )

            }

        }

        /* LOGIN CARD */

        Card(

            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.large),

            shape = MaterialTheme.shapes.large,

            elevation = CardDefaults.cardElevation(AppSpacing.extraSmall + AppSpacing.extraSmall),

            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )

        ) {

            Column(
                modifier = Modifier.padding(AppSpacing.large)
            ) {

                Text(
                    "Welcome Back",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(AppSpacing.large))

                /* Login Method */

                Row {

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { loginMethod = "email" }
                    ) {
                        Icon(Icons.Default.Email, null)
                        Spacer(modifier = Modifier.width(AppSpacing.extraSmall + AppSpacing.extraSmall))
                        Text("Email")
                    }

                    Spacer(modifier = Modifier.width(AppSpacing.small))

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { loginMethod = "phone" }
                    ) {
                        Icon(Icons.Default.Phone, null)
                        Spacer(modifier = Modifier.width(AppSpacing.extraSmall + AppSpacing.extraSmall))
                        Text("Phone")
                    }

                }

                Spacer(modifier = Modifier.height(AppSpacing.default))

                /* Email or Phone */

                if (loginMethod == "email") {

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Email, null)
                        }
                    )

                } else {

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Phone, null)
                        }
                    )

                }

                Spacer(modifier = Modifier.height(AppSpacing.medium))

                /* Password */

                if (loginMethod == "email") {

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showPassword)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {

                            IconButton(
                                onClick = { showPassword = !showPassword }
                            ) {

                                Icon(
                                    if (showPassword)
                                        Icons.Default.VisibilityOff
                                    else
                                        Icons.Default.Visibility,
                                    null
                                )

                            }

                        }
                    )

                }

                Spacer(modifier = Modifier.height(AppSpacing.large))

                /* Login Button */

                Button(

                    modifier = Modifier.fillMaxWidth(),

                    onClick = {

                        if (loginMethod == "email") {

                            if (email.isEmpty() || password.isEmpty()) {

                                error = "Fields cannot be empty"
                                return@Button

                            }

                            viewModel.login(email, password) { success, message ->

                                if (success) {

                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }

                                } else {

                                    error = message ?: "Login failed"

                                }

                            }

                        }

                    }

                ) {

                    Text(
                        if (loginMethod == "phone")
                            "Get OTP"
                        else
                            "Login"
                    )

                }

                Spacer(modifier = Modifier.height(AppSpacing.large))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(AppSpacing.large))

                /* Google Login */

                OutlinedButton(

                    modifier = Modifier.fillMaxWidth(),

                    onClick = {

                        launcher.launch(googleSignInClient.signInIntent)

                    }

                ) {

                    Icon(Icons.Default.AccountCircle, null)

                    Spacer(modifier = Modifier.width(AppSpacing.small))

                    Text("Continue with Google")

                }

                Spacer(modifier = Modifier.height(AppSpacing.medium))

                TextButton(

                    onClick = {

                        navController.navigate(Routes.REGISTER)

                    },

                    modifier = Modifier.align(Alignment.CenterHorizontally)

                ) {

                    Text("Create Account")

                }

                if (error.isNotEmpty()) {

                    Spacer(modifier = Modifier.height(AppSpacing.small))

                    Text(
                        error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                }

            }

        }

    }

}
