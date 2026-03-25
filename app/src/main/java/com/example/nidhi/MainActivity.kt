package com.example.nidhi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.nidhi.navigation.NavGraph
import com.example.nidhi.notifications.AppFirebaseMessagingService
import com.example.nidhi.notifications.PushTokenRegistrar
import com.example.nidhi.payment.RazorpayPaymentHandler
import com.example.nidhi.ui.theme.NidhiTheme
import com.example.nidhi.viewmodel.AuthViewModel
import com.google.firebase.FirebaseApp
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        PushTokenRegistrar.registerCurrentTokenIfLoggedIn()
        requestNotificationPermissionIfNeeded()

        // Preload Razorpay SDK assets for faster checkout launch.
        Checkout.preload(applicationContext)

        val deepLinkBookingId = intent
            ?.getStringExtra(AppFirebaseMessagingService.EXTRA_BOOKING_ID)
            ?.takeIf { it.isNotBlank() }

        // Monitor for session expiry and navigate back to login when it fires.
        lifecycleScope.launch {
            authViewModel.sessionExpired.collect { expired ->
                if (expired) {
                    authViewModel.clearSessionExpired()
                    // The NavGraph already checks FirebaseAuth.currentUser on each navigation
                    // event, so logging out is sufficient to redirect the user to the login screen.
                }
            }
        }

        setContent {
            NidhiTheme {
                NavGraph(deepLinkBookingId = deepLinkBookingId)
            }
        }
    }

    // ── Razorpay callbacks ──────────────────────────────────────────────────

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        if (razorpayPaymentId.isNullOrBlank()) return
        RazorpayPaymentHandler.handleSuccess(razorpayPaymentId)
    }

    override fun onPaymentError(errorCode: Int, errorDescription: String?, paymentData: PaymentData?) {
        RazorpayPaymentHandler.handleError(errorCode, errorDescription)
    }

    // ── Notification permission ─────────────────────────────────────────────

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}
