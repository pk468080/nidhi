package com.example.nidhi.ui.screens.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.example.nidhi.data.model.Booking
import com.example.nidhi.data.model.BookingStatus
import com.example.nidhi.ui.theme.AppSpacing
import com.example.nidhi.ui.theme.Success
import com.example.nidhi.ui.theme.Warning
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowChecklistScreen(navController: NavController) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val realtimeDb = remember { FirebaseDatabase.getInstance() }
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    var latestBooking by remember { mutableStateOf<Booking?>(null) }
    val logs = remember { mutableStateListOf<String>() }

    DisposableEffect(userId) {
        if (userId.isNullOrBlank()) {
            onDispose { }
        } else {
            val reg = firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        logs.add(logLine("Bookings listener error: ${error.message}"))
                        return@addSnapshotListener
                    }

                    val newest = snapshot?.documents
                        ?.mapNotNull { it.toObject(Booking::class.java) }
                        ?.maxByOrNull { it.timestamp }

                    if (newest?.bookingId != latestBooking?.bookingId) {
                        newest?.let { logs.add(logLine("Tracking booking ${it.bookingId.take(8)}...")) }
                    }

                    if (newest?.status != latestBooking?.status && newest != null) {
                        logs.add(logLine("Booking status -> ${newest.status}"))
                    }
                    latestBooking = newest
                }

            onDispose { reg.remove() }
        }
    }

    DisposableEffect(latestBooking?.bookingId) {
        val bookingId = latestBooking?.bookingId
        if (bookingId.isNullOrBlank()) {
            onDispose { }
        } else {
            val ref = realtimeDb.getReference("tracking/$bookingId")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.child("status").getValue(String::class.java)
                    val eta = snapshot.child("eta").getValue(Long::class.java)?.toInt()
                    val providerLat = snapshot.child("providerLat").getValue(Double::class.java)
                    val providerLng = snapshot.child("providerLng").getValue(Double::class.java)

                    if (!status.isNullOrBlank()) {
                        logs.add(logLine("Tracking status -> $status${eta?.let { " | ETA: ${it}m" } ?: ""}"))
                    }

                    if (providerLat != null && providerLng != null && !providerLat.isNaN() && !providerLng.isNaN()) {
                        logs.add(logLine("Provider location -> Lat: ${String.format(Locale.getDefault(), "%.4f", providerLat)}, Lng: ${String.format(Locale.getDefault(), "%.4f", providerLng)}"))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    logs.add(logLine("Tracking listener cancelled: ${error.message}"))
                }
            }
            ref.addValueEventListener(listener)

            onDispose { ref.removeEventListener(listener) }
        }
    }

    val status = latestBooking?.status.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flow Checklist") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AppSpacing.default),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = AppSpacing.extraSmall)
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    Text(
                        text = "End-to-End Verification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Booking: ${latestBooking?.bookingId?.take(12) ?: "Not created yet"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                    ChecklistRow("1. Booking created", latestBooking != null)
                    ChecklistRow(
                        "2. Provider accepted",
                        status in listOf(
                            BookingStatus.ACCEPTED.value,
                            BookingStatus.ON_THE_WAY.value,
                            BookingStatus.ARRIVED.value,
                            BookingStatus.COMPLETED.value
                        )
                    )
                    ChecklistRow(
                        "3. Provider on the way",
                        status in listOf(
                            BookingStatus.ON_THE_WAY.value,
                            BookingStatus.ARRIVED.value,
                            BookingStatus.COMPLETED.value
                        )
                    )
                    ChecklistRow(
                        "4. Provider arrived",
                        status in listOf(
                            BookingStatus.ARRIVED.value,
                            BookingStatus.COMPLETED.value
                        )
                    )
                }
            }

            Text(
                text = "Live Logs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (logs.isEmpty()) {
                Text(
                    "No events yet. Create and progress a booking to see logs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                    items(logs.takeLast(30).reversed()) { line ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = line,
                                modifier = Modifier.padding(AppSpacing.small),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
            Text(
                text = "Use this temporary debug screen to validate: booking -> accepted -> on_the_way -> arrived.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChecklistRow(label: String, done: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            if (done) "DONE" else "PENDING",
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) Success else Warning
        )
    }
}

private fun logLine(message: String): String {
    val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    return "[$ts] $message"
}
