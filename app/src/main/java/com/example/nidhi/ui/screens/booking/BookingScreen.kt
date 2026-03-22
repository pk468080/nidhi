package com.example.nidhi.ui.screens.booking

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nidhi.data.model.Booking
import com.example.nidhi.navigation.Routes
import com.example.nidhi.viewmodel.DEFAULT_PROVIDER_LAT
import com.example.nidhi.viewmodel.DEFAULT_PROVIDER_LNG
import com.example.nidhi.viewmodel.DEFAULT_PROVIDER_NAME
import com.example.nidhi.viewmodel.DEFAULT_PROVIDER_PHONE
import com.example.nidhi.viewmodel.DEFAULT_PROVIDER_RATING
import com.example.nidhi.viewmodel.INITIAL_ETA_MINUTES
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun BookingScreen(
    serviceName: String,
    navController: NavController
) {

    var address by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val firestore = FirebaseFirestore.getInstance()
    val database = FirebaseDatabase.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text(
            text = "Book $serviceName",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Service Address") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Preferred Date") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = time,
            onValueChange = { time = it },
            label = { Text("Preferred Time") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {

                if (address.isEmpty()) {
                    message = "Please enter address"
                    return@Button
                }

                val booking = Booking(
                    serviceName = serviceName,
                    address = address,
                    userId = userId,
                    providerName = DEFAULT_PROVIDER_NAME,
                    providerPhone = DEFAULT_PROVIDER_PHONE,
                    providerRating = DEFAULT_PROVIDER_RATING
                )

                firestore.collection("bookings")
                    .add(booking)
                    .addOnSuccessListener { docRef ->

                        message = "Booking Confirmed!"

                        // Write initial tracking data to Realtime Database so
                        // TrackingViewModel can begin receiving live updates immediately
                        val safeServiceName = serviceName.replace(" ", "_")
                        val trackingRef = database.getReference("tracking/$userId/$safeServiceName")
                        val initialTracking = mapOf(
                            "status" to "accepted",
                            "eta" to INITIAL_ETA_MINUTES,
                            "providerLat" to DEFAULT_PROVIDER_LAT,
                            "providerLng" to DEFAULT_PROVIDER_LNG,
                            "providerName" to DEFAULT_PROVIDER_NAME,
                            "providerPhone" to DEFAULT_PROVIDER_PHONE,
                            "providerRating" to DEFAULT_PROVIDER_RATING
                        )
                        trackingRef.setValue(initialTracking)

                        navController.navigate(
                            Routes.TRACKING + "/$serviceName"
                        )

                    }
                    .addOnFailureListener {

                        message = "Booking failed"

                    }

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Confirm Booking")
        }

        if (message.isNotEmpty()) {

            Spacer(modifier = Modifier.height(20.dp))

            Text(message)

        }

    }
}