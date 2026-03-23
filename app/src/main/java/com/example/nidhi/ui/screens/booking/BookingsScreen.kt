package com.example.nidhi.ui.screens.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nidhi.data.model.Booking
import com.example.nidhi.data.model.BookingStatus
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
                    bookings = it.documents
                        .mapNotNull { doc -> doc.toObject(Booking::class.java) }
                        .sortedByDescending { it.timestamp }
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

        if (bookings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("No bookings yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bookings) { booking ->
                    BookingListCard(booking = booking, navController = navController)
                }
            }
        }
    }
}

@Composable
private fun BookingListCard(booking: Booking, navController: NavController) {
    val statusColor = when (booking.status) {
        BookingStatus.PENDING.value -> Color(0xFFF57F17)
        BookingStatus.ACCEPTED.value, BookingStatus.ON_THE_WAY.value -> Color(0xFF1565C0)
        BookingStatus.ARRIVED.value, BookingStatus.COMPLETED.value -> Color(0xFF2E7D32)
        BookingStatus.REJECTED.value, BookingStatus.CANCELLED.value -> Color(0xFFC62828)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (booking.bookingId.isNotBlank()) {
                    navController.navigate(Routes.BOOKING_DETAILS + "/${booking.bookingId}")
                }
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = booking.serviceName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (booking.amount > 0) {
                    Text(
                        text = "₹${"%.0f".format(booking.amount)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (booking.address.isNotEmpty()) {
                Text(
                    text = booking.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1
                )
            }

            if (booking.scheduledDate.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${booking.scheduledDate}${if (booking.scheduledTime.isNotEmpty()) " at ${booking.scheduledTime}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = statusColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            ) {
                val statusLabel = BookingStatus.values()
                    .find { it.value == booking.status }?.displayName
                    ?: booking.status.replaceFirstChar { it.uppercase() }
                Text(
                    text = statusLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = statusColor
                )
            }
        }
    }
}
