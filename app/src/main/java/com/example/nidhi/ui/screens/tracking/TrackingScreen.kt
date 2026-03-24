package com.example.nidhi.ui.screens.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nidhi.data.model.BookingStatus
import com.example.nidhi.ui.theme.AppSpacing
import com.example.nidhi.ui.theme.Info
import com.example.nidhi.ui.theme.Success
import com.example.nidhi.ui.theme.Warning
import com.example.nidhi.ui.theme.Orange400
import com.example.nidhi.viewmodel.DEFAULT_PROVIDER_LAT
import com.example.nidhi.viewmodel.DEFAULT_PROVIDER_LNG
import com.example.nidhi.viewmodel.TrackingViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun TrackingScreen(bookingId: String) {

    val viewModel: TrackingViewModel = viewModel()
    val trackingData by viewModel.trackingData.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember(context) { LocationServices.getFusedLocationProviderClient(context) }

    var requestedLocationPermission by remember { mutableStateOf(false) }
    var customerLocation by remember { mutableStateOf(LatLng(28.6139, 77.2090)) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionResult ->
        val granted = permissionResult[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissionResult[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar("Location permission denied. Showing fallback destination.")
            }
        }
    }

    LaunchedEffect(bookingId) {
        viewModel.startTracking(bookingId)
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission(context) && !requestedLocationPermission) {
            requestedLocationPermission = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    DisposableEffect(hasLocationPermission(context)) {
        if (!hasLocationPermission(context)) {
            onDispose { }
        } else {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                .setMinUpdateIntervalMillis(2500L)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    customerLocation = LatLng(location.latitude, location.longitude)
                    viewModel.updateCustomerLocation(location.latitude, location.longitude)
                }
            }

            @android.annotation.SuppressLint("MissingPermission")
            fun startUpdates() {
                fusedLocationClient.requestLocationUpdates(request, callback, android.os.Looper.getMainLooper())
            }

            startUpdates()
            onDispose { fusedLocationClient.removeLocationUpdates(callback) }
        }
    }

    val userLocation = customerLocation
    val hasProviderLocation = !trackingData.providerLat.isNaN() && !trackingData.providerLng.isNaN()
    val providerLocation = if (hasProviderLocation) {
        LatLng(trackingData.providerLat, trackingData.providerLng)
    } else {
        LatLng(
            DEFAULT_PROVIDER_LAT.takeIf { !it.isNaN() } ?: userLocation.latitude,
            DEFAULT_PROVIDER_LNG.takeIf { !it.isNaN() } ?: userLocation.longitude
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(providerLocation, 14f)
    }

    // Animate camera to follow provider location updates
    LaunchedEffect(trackingData.providerLat, trackingData.providerLng) {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLng(providerLocation),
            durationMs = 1000
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { scaffoldPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {

        // Full-screen map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        ) {
            Marker(
                state = MarkerState(position = userLocation),
                title = "Your Location",
                snippet = "Destination"
            )
            if (hasProviderLocation) {
                Marker(
                    state = MarkerState(position = providerLocation),
                    title = trackingData.providerName,
                    snippet = "Service Provider"
                )
                Polyline(
                    points = listOf(providerLocation, userLocation),
                    color = Info,
                    width = 8f
                )
            }
        }

        // Offline banner
        AnimatedVisibility(
            visible = trackingData.isOffline,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.medium),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "⚠️  You are offline - showing last known location",
                    modifier = Modifier.padding(AppSpacing.medium),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Bottom info card overlaid on the map
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.default),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = AppSpacing.large)
        ) {
            Column(
                modifier = Modifier.padding(AppSpacing.large)
            ) {

                // Status chip
                TrackingStatusChip(status = trackingData.status)

                Spacer(modifier = Modifier.height(AppSpacing.default))

                // ETA + distance
                if (trackingData.status == BookingStatus.PENDING.value) {
                    Text(
                        text = "Waiting for provider acceptance",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedContent(
                            targetState = trackingData.eta,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "eta"
                        ) { eta ->
                            Text(
                                text = "$eta min",
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(AppSpacing.small))
                        Text(
                            text = "away",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!trackingData.distanceKm.isNaN()) {
                            Spacer(modifier = Modifier.width(AppSpacing.medium))
                            Text(
                                text = "(${formatDistance(trackingData.distanceKm)})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.default))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(AppSpacing.default))

                // Provider info row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(AppSpacing.xxxLarge)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = trackingData.providerName.firstOrNull()?.toString() ?: "P",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(AppSpacing.medium))
                        Column {
                            Text(
                                text = trackingData.providerName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Warning,
                                    modifier = Modifier.size(AppSpacing.default)
                                )
                                Spacer(modifier = Modifier.width(AppSpacing.extraSmall))
                                Text(
                                    text = "${trackingData.providerRating}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Call button
                    FilledIconButton(onClick = { /* launch dialer */ }) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Call ${trackingData.providerName}"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.small))

                Text(
                    text = "Service: ${trackingData.serviceName.ifBlank { "Assigned service" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    }
}

/** Formats a distance value in km to a human-readable string. */
private fun formatDistance(distanceKm: Double): String =
    if (distanceKm < 1.0) {
        "${(distanceKm * 1000).toInt()} m"
    } else {
        String.format(Locale.getDefault(), "%.1f km", distanceKm)
    }

private fun hasLocationPermission(context: Context): Boolean {
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

@Composable
private fun TrackingStatusChip(status: String) {
    val (label, backgroundColor, contentColor) = when (status) {
        "pending" -> Triple("⏳  Waiting for Provider", Warning.copy(alpha = 0.1f), Warning)
        "accepted" -> Triple("✓  Booking Accepted", Info.copy(alpha = 0.1f), Info)
        "on_the_way" -> Triple("🚗  Provider On the Way", Orange400.copy(alpha = 0.1f), Orange400)
        "arrived" -> Triple("📍  Provider Arrived", Success.copy(alpha = 0.1f), Success)
        "completed" -> Triple("✅  Service Completed", Success.copy(alpha = 0.1f), Success)
        else -> Triple(status, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
            color = contentColor,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

