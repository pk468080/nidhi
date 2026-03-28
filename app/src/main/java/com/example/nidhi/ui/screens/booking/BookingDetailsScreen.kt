package com.example.nidhi.ui.screens.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.example.nidhi.data.model.Booking
import com.example.nidhi.data.model.BookingStatus
import com.example.nidhi.data.repository.BookingRepository
import com.example.nidhi.navigation.Routes
import com.example.nidhi.ui.theme.AppSpacing
import com.example.nidhi.ui.theme.Error
import com.example.nidhi.ui.theme.Info
import com.example.nidhi.ui.theme.Success
import com.example.nidhi.ui.theme.Warning
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    bookingId: String,
    navController: NavController
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val bookingRepository = remember { BookingRepository() }

    var booking by remember { mutableStateOf<Booking?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isCancelling by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Use DisposableEffect so the Firestore listener is cleaned up when the
    // screen leaves the composition, preventing a billing / memory leak.
    if (bookingId.isBlank()) {
        isLoading = false
    } else {
        DisposableEffect(bookingId) {
            val registration = bookingRepository.observeBookingById(bookingId) { loaded ->
                isLoading = false
                booking = loaded?.takeIf { it.userId == userId }
            }
            onDispose { registration.remove() }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Booking") },
            text = { Text("Are you sure you want to cancel this booking?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        isCancelling = true
                        bookingRepository.cancelBooking(bookingId) { success ->
                            isCancelling = false
                            if (!success) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed to cancel booking. Please try again.")
                                }
                            }
                        }
                    }
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Booking")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    .padding(AppSpacing.default)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {

                // Service header card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(AppSpacing.large),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(AppSpacing.xxxLarge + AppSpacing.extraSmall)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppSpacing.extraLarge + AppSpacing.extraSmall)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(AppSpacing.default))
                        Column {
                            Text(
                                text = booking?.serviceName ?: "Booking",
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
                        BookingDetailRow(Icons.AutoMirrored.Filled.Notes, "Notes", b.notes)
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.small))

                    val canTrackProvider = b.status in listOf(
                        BookingStatus.ACCEPTED.value,
                        BookingStatus.ON_THE_WAY.value,
                        BookingStatus.ARRIVED.value
                    )

                    if (canTrackProvider) {
                        Button(
                            onClick = {
                                navController.navigate(Routes.TRACKING + "/${b.bookingId}")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppSpacing.xxxLarge + AppSpacing.extraSmall),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(AppSpacing.small))
                            Text(
                                text = "Track Provider",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else if (b.status == BookingStatus.COMPLETED.value) {
                        Button(
                            onClick = {
                                navController.navigate(Routes.REVIEW + "/${b.bookingId}")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppSpacing.xxxLarge + AppSpacing.extraSmall),
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(modifier = Modifier.width(AppSpacing.small))
                            Text(
                                text = "Leave a Review",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else if (b.status == BookingStatus.PENDING.value) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text("Waiting for provider to accept your request") },
                            leadingIcon = {
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null)
                            }
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.small))
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCancelling,
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            if (isCancelling) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Cancel, contentDescription = null)
                                Spacer(modifier = Modifier.width(AppSpacing.small))
                                Text("Cancel Booking")
                            }
                        }
                    }

                } ?: run {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.xxxLarge - AppSpacing.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No booking found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingStatusChip(status: String) {
    val bookingStatus = BookingStatus.entries.find { it.value == status }
    val (backgroundColor, contentColor) = when (status) {
        BookingStatus.PENDING.value -> Pair(
            Warning.copy(alpha = 0.1f),
            Warning
        )
        BookingStatus.ACCEPTED.value,
        BookingStatus.ON_THE_WAY.value -> Pair(
            Info.copy(alpha = 0.1f),
            Info
        )
        BookingStatus.ARRIVED.value,
        BookingStatus.COMPLETED.value -> Pair(
            Success.copy(alpha = 0.1f),
            Success
        )
        BookingStatus.REJECTED.value,
        BookingStatus.CANCELLED.value -> Pair(
            Error.copy(alpha = 0.1f),
            Error
        )
        else -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = bookingStatus?.displayName ?: status.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = AppSpacing.default, vertical = AppSpacing.small),
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
                .size(AppSpacing.large)
                .padding(top = AppSpacing.extraSmall / 2),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(AppSpacing.medium))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
