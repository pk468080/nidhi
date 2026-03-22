package com.example.nidhi.ui.screens.tracking

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nidhi.viewmodel.DEFAULT_PROVIDER_LAT
import com.example.nidhi.viewmodel.DEFAULT_PROVIDER_LNG
import com.example.nidhi.viewmodel.TrackingData
import com.example.nidhi.viewmodel.TrackingViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

@Composable
fun TrackingScreen(serviceName: String) {

    val viewModel: TrackingViewModel = viewModel()
    val trackingData by viewModel.trackingData.collectAsState()

    LaunchedEffect(serviceName) {
        viewModel.startTracking(serviceName)
    }

    val userLocation = LatLng(28.6139, 77.2090)
    val providerLocation = LatLng(
        trackingData.providerLat.takeIf { !it.isNaN() } ?: DEFAULT_PROVIDER_LAT,
        trackingData.providerLng.takeIf { !it.isNaN() } ?: DEFAULT_PROVIDER_LNG
    )

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

    Box(modifier = Modifier.fillMaxSize()) {

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
            Marker(
                state = MarkerState(position = providerLocation),
                title = trackingData.providerName,
                snippet = "Service Provider"
            )
        }

        // Bottom info card overlaid on the map
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                // Status chip
                TrackingStatusChip(status = trackingData.status)

                Spacer(modifier = Modifier.height(16.dp))

                // ETA
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "away",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))

                // Provider info row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = trackingData.providerName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
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

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Service: ${serviceName.replace("_", " ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun TrackingStatusChip(status: String) {
    val (label, backgroundColor, contentColor) = when (status) {
        "accepted" -> Triple("✓  Booking Accepted", Color(0xFFE3F2FD), Color(0xFF1565C0))
        "on_the_way" -> Triple("🚗  Provider On the Way", Color(0xFFFFF3E0), Color(0xFFE65100))
        "arrived" -> Triple("📍  Provider Arrived", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "completed" -> Triple("✅  Service Completed", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        else -> Triple(status, Color(0xFFF5F5F5), Color.DarkGray)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}