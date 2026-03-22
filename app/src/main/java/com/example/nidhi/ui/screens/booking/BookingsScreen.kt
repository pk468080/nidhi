package com.example.nidhi.ui.screens.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nidhi.data.model.Booking
import com.example.nidhi.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun BookingsScreen(navController: NavController) {

    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    var bookings by remember { mutableStateOf(listOf<Booking>()) }

    LaunchedEffect(Unit) {

        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->

                snapshot?.let {

                    bookings = it.documents.mapNotNull { doc ->
                        doc.toObject(Booking::class.java)
                    }

                }

            }

    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text(
            text = "My Bookings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn {

            items(bookings) { booking ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {

                            navController.navigate(
                                Routes.BOOKING_DETAILS + "/${booking.serviceName.replace(" ", "_")}"
                            )

                        }
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = booking.serviceName,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Address: ${booking.address}")

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Status: ${booking.status}")

                    }

                }

            }

        }

    }

}