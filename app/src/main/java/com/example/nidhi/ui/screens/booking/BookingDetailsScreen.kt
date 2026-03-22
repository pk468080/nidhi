package com.example.nidhi.ui.screens.booking

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nidhi.navigation.Routes

@Composable
fun BookingDetailsScreen(
    serviceName: String,
    navController: NavController
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text(
            text = "Booking Details",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text("Service: $serviceName")

        Spacer(modifier = Modifier.height(10.dp))

        Text("Status: Pending")

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {

                navController.navigate(
                    Routes.TRACKING + "/$serviceName"
                )

            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Track Service")

        }

    }

}