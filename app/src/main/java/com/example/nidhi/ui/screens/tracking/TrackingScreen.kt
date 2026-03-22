package com.example.nidhi.ui.screens.tracking

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.*

@Composable
fun TrackingScreen(serviceName: String) {

    val userLocation = LatLng(28.6139, 77.2090) // Delhi example
    val technicianLocation = LatLng(28.6200, 77.2150)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 13f)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),

            cameraPositionState = cameraPositionState
        ) {

            Marker(
                state = MarkerState(position = userLocation),
                title = "Your Location"
            )

            Marker(
                state = MarkerState(position = technicianLocation),
                title = "Technician"
            )

        }

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = "Tracking $serviceName",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("Technician assigned")

            Spacer(modifier = Modifier.height(10.dp))

            Text("ETA: 15 minutes")

        }

    }
}