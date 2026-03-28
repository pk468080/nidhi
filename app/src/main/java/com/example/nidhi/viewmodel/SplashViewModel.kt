package com.example.nidhi.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SplashDestination {
    object Home : SplashDestination()
    object Login : SplashDestination()
    object Waiting : SplashDestination()
}

class SplashViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Waiting)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    /**
     * Resolves the post-splash navigation destination.
     * Called once the splash animation delay has elapsed so the decision is
     * made in the ViewModel rather than directly in the Composable.
     */
    fun resolveDestination() {
        _destination.value = if (auth.currentUser != null) {
            SplashDestination.Home
        } else {
            SplashDestination.Login
        }
    }
}

