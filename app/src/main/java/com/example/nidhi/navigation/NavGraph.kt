package com.example.nidhi.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.example.nidhi.ui.screens.auth.LoginScreen
import com.example.nidhi.ui.screens.auth.RegisterScreen
import com.example.nidhi.ui.screens.booking.BookingDetailsScreen
import com.example.nidhi.ui.screens.booking.BookingScreen
import com.example.nidhi.ui.screens.main.MainScreen
import com.example.nidhi.ui.screens.services.ServiceDetailsScreen
import com.example.nidhi.ui.screens.splash.SplashScreen
import com.example.nidhi.ui.screens.tracking.TrackingScreen

@Composable
fun NavGraph() {

    val navController = rememberNavController()

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

        composable(Routes.HOME) {
            MainScreen(navController)
        }

        composable(
            route = Routes.SERVICE_DETAILS + "/{serviceName}"
        ) { backStackEntry ->

            val serviceName = backStackEntry.arguments?.getString("serviceName") ?: ""

            ServiceDetailsScreen(
                navController = navController,
                serviceName = serviceName
            )

        }
        composable(Routes.BOOKING + "/{serviceName}") { backStackEntry ->

            val serviceName = backStackEntry.arguments?.getString("serviceName") ?: ""

            BookingScreen(
                serviceName = serviceName,
                navController = navController
            )

        }
        composable(Routes.TRACKING + "/{serviceName}") { backStackEntry ->

            val serviceName = backStackEntry.arguments?.getString("serviceName") ?: ""

            TrackingScreen(serviceName)

        }
        composable(
            route = Routes.BOOKING_DETAILS + "/{serviceName}"
        ) { backStackEntry ->

            val serviceName =
                backStackEntry.arguments?.getString("serviceName") ?: ""

            BookingDetailsScreen(
                serviceName = serviceName,
                navController = navController
            )

        }


    }
}