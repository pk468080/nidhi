package com.example.nidhi.ui.screens.splash

import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.example.nidhi.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()

    LaunchedEffect(Unit) {

        delay(2000)

        if (auth.currentUser != null) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        } else {
            navController.navigate(Routes.LOGIN) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        }
    }
}