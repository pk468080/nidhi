package com.example.nidhi.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.nidhi.navigation.Routes
import com.example.nidhi.ui.screens.home.HomeScreen
import com.example.nidhi.ui.screens.booking.BookingsScreen
import com.example.nidhi.ui.screens.profile.ProfileScreen

@Composable
fun MainScreen(rootNavController: NavController) {

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(

        bottomBar = {

            NavigationBar {

                NavigationBarItem(
                    selected = currentRoute == Routes.HOME,
                    onClick = {
                        navController.navigate(Routes.HOME) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                        }
                    },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home") }
                )

                NavigationBarItem(
                    selected = currentRoute == Routes.BOOKINGS,
                    onClick = {
                        navController.navigate(Routes.BOOKINGS) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                    label = { Text("Bookings") }
                )

                NavigationBarItem(
                    selected = currentRoute == Routes.PROFILE,
                    onClick = {
                        navController.navigate(Routes.PROFILE) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                        }
                    },
                    icon = { Icon(Icons.Default.Person, null) },
                    label = { Text("Profile") }
                )

            }

        }

    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {

            composable(Routes.HOME) { 
                HomeScreen(rootNavController) 
            }

            composable(Routes.BOOKINGS) { 
                BookingsScreen(rootNavController) 
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onLogout = {
                        rootNavController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    navController = rootNavController
                )
            }

        }

    }
}