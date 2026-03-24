package com.example.nidhi.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.example.nidhi.ui.screens.auth.LoginScreen
import com.example.nidhi.ui.screens.auth.OTPVerificationScreen
import com.example.nidhi.ui.screens.auth.RegisterScreen
import com.example.nidhi.ui.screens.booking.BookingDetailsScreen
import com.example.nidhi.ui.screens.booking.BookingScreen
import com.example.nidhi.ui.screens.booking.ReviewScreen
import com.example.nidhi.ui.screens.debug.FlowChecklistScreen
import com.example.nidhi.ui.screens.main.MainScreen
import com.example.nidhi.ui.screens.payment.PaymentScreen
import com.example.nidhi.ui.screens.payment.TransactionHistoryScreen
import com.example.nidhi.ui.screens.provider.ProviderPanelScreen
import com.example.nidhi.ui.screens.services.SearchScreen
import com.example.nidhi.ui.screens.services.ServiceDetailsScreen
import com.example.nidhi.ui.screens.splash.SplashScreen
import com.example.nidhi.ui.screens.tracking.TrackingScreen

@Composable
fun NavGraph(deepLinkBookingId: String? = null) {

    val navController = rememberNavController()

    LaunchedEffect(deepLinkBookingId) {
        if (!deepLinkBookingId.isNullOrBlank()) {
            navController.navigate(Routes.BOOKING_DETAILS + "/$deepLinkBookingId")
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {

        composable(Routes.SPLASH) {
            SplashScreen(navController)
        }

        composable(Routes.LOGIN) {
            LoginScreen(navController)
        }

        composable(Routes.REGISTER) {
            RegisterScreen(navController)
        }

        composable(Routes.OTP_VERIFICATION + "/{phoneNumber}") { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            OTPVerificationScreen(phoneNumber = phoneNumber, navController = navController)
        }

        composable(Routes.HOME) {
            MainScreen(navController)
        }

        composable(Routes.SEARCH) {
            SearchScreen(navController)
        }

        composable(
            route = Routes.SERVICE_DETAILS + "/{serviceName}"
        ) { backStackEntry ->
            val serviceName = backStackEntry.arguments?.getString("serviceName") ?: ""
            ServiceDetailsScreen(navController = navController, serviceName = serviceName)
        }

        composable(Routes.BOOKING + "/{serviceName}") { backStackEntry ->
            val serviceName = backStackEntry.arguments?.getString("serviceName") ?: ""
            BookingScreen(serviceName = serviceName, navController = navController)
        }

        composable(Routes.TRACKING + "/{bookingId}") { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            TrackingScreen(bookingId = bookingId)
        }

        composable(
            route = Routes.BOOKING_DETAILS + "/{bookingId}"
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            BookingDetailsScreen(bookingId = bookingId, navController = navController)
        }

        composable(
            route = Routes.PAYMENT + "/{bookingId}/{serviceName}/{amount}"
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            val serviceName = backStackEntry.arguments?.getString("serviceName") ?: ""
            val amount = backStackEntry.arguments?.getString("amount")?.toDoubleOrNull() ?: 0.0
            PaymentScreen(
                bookingId = bookingId,
                serviceName = serviceName,
                amount = amount,
                navController = navController
            )
        }

        composable(Routes.TRANSACTIONS) {
            TransactionHistoryScreen(navController)
        }

        composable(Routes.PROVIDER_PANEL) {
            ProviderPanelScreen(navController = navController)
        }

        composable(Routes.FLOW_CHECKLIST) {
            FlowChecklistScreen(navController = navController)
        }

        composable(
            route = Routes.REVIEW + "/{bookingId}"
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            ReviewScreen(bookingId = bookingId, navController = navController)
        }
    }
}

