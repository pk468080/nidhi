package com.example.nidhi.ui.screens.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nidhi.data.model.Booking
import com.example.nidhi.data.model.BookingStatus
import com.example.nidhi.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    serviceName: String,
    navController: NavController
) {
    val displayName = serviceName.replace("_", " ")
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val firestore = FirebaseFirestore.getInstance()

    var booking by remember { mutableStateOf<Booking?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(serviceName) {
        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .whereEqualTo("serviceName", displayName)
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                booking = snapshot?.documents
                    ?.mapNotNull { it.toObject(Booking::class.java) }
                    ?.maxByOrNull { it.timestamp }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Service header card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            booking?.let { b ->
                                if (b.scheduledDate.isNotEmpty()) {
                                    Text(
                                        text = "${b.scheduledDate}${if (b.scheduledTime.isNotEmpty()) " at ${b.scheduledTime}" else ""}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                booking?.let { b ->

                    // Status chip
                    BookingStatusChip(status = b.status)

                    // Details
                    if (b.address.isNotEmpty()) {
                        BookingDetailRow(Icons.Default.LocationOn, "Address", b.address)
                    }
                    if (b.providerName.isNotEmpty()) {
                        BookingDetailRow(Icons.Default.Person, "Provider", b.providerName)
                    }
                    if (b.providerPhone.isNotEmpty()) {
                        BookingDetailRow(Icons.Default.Phone, "Contact", b.providerPhone)
                    }
                    if (b.amount > 0) {
                        BookingDetailRow(Icons.Default.CurrencyRupee, "Amount", "₹${b.amount.toInt()}")
                    }
                    BookingDetailRow(
                        Icons.Default.Payment,
                        "Payment",
                        b.paymentStatus.replaceFirstChar { it.uppercase() }
                    )
                    if (b.notes.isNotEmpty()) {
                        BookingDetailRow(Icons.Default.Notes, "Notes", b.notes)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val isActive = b.status !in listOf(
                        BookingStatus.CANCELLED.value,
                        BookingStatus.COMPLETED.value,
                        BookingStatus.REJECTED.value
                    )

                    if (isActive) {
                        Button(
                            onClick = {
                                navController.navigate(Routes.TRACKING + "/$serviceName")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Track Provider",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                } ?: run {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No booking found for $displayName",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingStatusChip(status: String) {
    val bookingStatus = BookingStatus.values().find { it.value == status }
    val (backgroundColor, contentColor) = when (status) {
        BookingStatus.PENDING.value -> Pair(Color(0xFFFFF8E1), Color(0xFFF57F17))
        BookingStatus.ACCEPTED.value,
        BookingStatus.ON_THE_WAY.value -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        BookingStatus.ARRIVED.value,
        BookingStatus.COMPLETED.value -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        BookingStatus.REJECTED.value,
        BookingStatus.CANCELLED.value -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        else -> Pair(Color(0xFFF5F5F5), Color.DarkGray)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = bookingStatus?.displayName ?: status.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun BookingDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}
