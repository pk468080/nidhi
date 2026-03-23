package com.example.nidhi.ui.screens.provider

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.nidhi.data.model.Booking
import com.example.nidhi.data.model.BookingStatus
import com.example.nidhi.viewmodel.ProviderPanelViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderPanelScreen(navController: NavController) {
    val viewModel: ProviderPanelViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember(context) { LocationServices.getFusedLocationProviderClient(context) }
    var pendingLiveBookingId by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionResult ->
        val granted = permissionResult[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissionResult[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            pendingLiveBookingId?.let { viewModel.startLiveTracking(it) }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Location permission is required for live tracking")
            }
        }
        pendingLiveBookingId = null
    }

    LaunchedEffect(Unit) {
        viewModel.startListening()
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val liveBookingId = state.liveTrackingBookingId
    DisposableEffect(liveBookingId) {
        if (liveBookingId.isNullOrBlank()) {
            onDispose { }
        } else if (!hasLocationPermission(context)) {
            scope.launch { snackbarHostState.showSnackbar("Enable location permission to continue live tracking") }
            viewModel.stopLiveTracking(liveBookingId)
            onDispose { }
        } else {
            val request = com.google.android.gms.location.LocationRequest
                .Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                .setMinUpdateIntervalMillis(2500L)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    viewModel.updateLiveProviderLocation(
                        bookingId = liveBookingId,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                }
            }

            @SuppressLint("MissingPermission")
            fun requestUpdates() {
                fusedLocationClient.requestLocationUpdates(
                    request,
                    callback,
                    android.os.Looper.getMainLooper()
                )
            }

            requestUpdates()

            onDispose {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Provider Panel (Temporary)") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(start = 24.dp))
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Pending Requests",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.pendingBookings.isEmpty()) {
                item { EmptyState("No pending requests") }
            } else {
                items(state.pendingBookings) { booking ->
                    ProviderBookingCard(
                        booking = booking,
                        onAccept = { viewModel.acceptBooking(booking) },
                        onReject = { viewModel.rejectBooking(booking.bookingId) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Active Jobs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.activeBookings.isEmpty()) {
                item { EmptyState("No active jobs") }
            } else {
                items(state.activeBookings) { booking ->
                    ActiveBookingCard(
                        booking = booking,
                        isLiveTracking = state.liveTrackingBookingId == booking.bookingId,
                        onOnTheWay = { viewModel.markOnTheWay(booking) },
                        onArrived = { viewModel.markArrived(booking) },
                        onComplete = { viewModel.markCompleted(booking.bookingId) },
                        onBumpLocation = { viewModel.bumpProviderLocation(booking) },
                        onToggleLiveTracking = {
                            if (state.liveTrackingBookingId == booking.bookingId) {
                                viewModel.stopLiveTracking(booking.bookingId)
                            } else {
                                if (hasLocationPermission(context)) {
                                    viewModel.startLiveTracking(booking.bookingId)
                                } else {
                                    pendingLiveBookingId = booking.bookingId
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderBookingCard(
    booking: Booking,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(booking.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            BookingMetaRow(icon = Icons.Default.Person, label = "Customer", value = booking.userId.take(8))
            BookingMetaRow(icon = Icons.Default.LocationOn, label = "Address", value = booking.address)
            BookingMetaRow(
                icon = Icons.Default.Schedule,
                label = "Schedule",
                value = "${booking.scheduledDate} ${booking.scheduledTime}".trim()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                    Text("Accept")
                }
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
private fun ActiveBookingCard(
    booking: Booking,
    isLiveTracking: Boolean,
    onOnTheWay: () -> Unit,
    onArrived: () -> Unit,
    onComplete: () -> Unit,
    onBumpLocation: () -> Unit,
    onToggleLiveTracking: () -> Unit
) {
    val statusColor = when (booking.status) {
        BookingStatus.ACCEPTED.value -> Color(0xFF1565C0)
        BookingStatus.ON_THE_WAY.value -> Color(0xFFE65100)
        BookingStatus.ARRIVED.value -> Color(0xFF2E7D32)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(booking.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(booking.status.replace("_", " ").replaceFirstChar { it.uppercase() })
                    }
                )
            }
            Text(booking.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            if (booking.status == BookingStatus.ACCEPTED.value) {
                TextButton(onClick = onOnTheWay, modifier = Modifier.fillMaxWidth()) {
                    Text("Mark On The Way", color = statusColor)
                }
            }

            if (booking.status == BookingStatus.ON_THE_WAY.value) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onBumpLocation, modifier = Modifier.weight(1f)) {
                        Text("Update Location")
                    }
                    Button(onClick = onArrived, modifier = Modifier.weight(1f)) {
                        Text("Mark Arrived")
                    }
                }

                OutlinedButton(onClick = onToggleLiveTracking, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isLiveTracking) "Stop Live Tracking" else "Start Live Tracking")
                }
            }

            if (booking.status == BookingStatus.ARRIVED.value) {
                Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                    Text("Complete Service")
                }
            }
        }
    }
}

@Composable
private fun BookingMetaRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = Color.Gray)
        Text(text = "$label: $value", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
private fun EmptyState(text: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun hasLocationPermission(context: android.content.Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}


