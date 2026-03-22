package com.example.nidhi.ui.screens.services

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nidhi.navigation.Routes

data class ServiceInfo(
    val description: String,
    val price: String,
    val features: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(serviceName: String, navController: NavController) {

    // Replace underscores back to spaces to match the keys
    val displayServiceName = serviceName.replace("_", " ")

    val serviceData = when (displayServiceName) {

        "AC Repair" -> ServiceInfo(
            description = "Professional AC repair and maintenance service at your doorstep.",
            price = "Starting from ₹299",
            features = listOf(
                "AC gas refill",
                "Cooling issue repair",
                "AC installation",
                "AC servicing"
            )
        )

        "Plumber" -> ServiceInfo(
            description = "Expert plumbing solutions for home and office.",
            price = "Starting from ₹199",
            features = listOf(
                "Tap repair",
                "Leak fixing",
                "Pipe installation",
                "Bathroom fitting"
            )
        )

        "Electrician" -> ServiceInfo(
            description = "Certified electricians for all electrical issues.",
            price = "Starting from ₹249",
            features = listOf(
                "Switch repair",
                "Fan installation",
                "Wiring issues",
                "Light installation"
            )
        )

        "Cleaning" -> ServiceInfo(
            description = "Deep cleaning services for homes and offices.",
            price = "Starting from ₹399",
            features = listOf(
                "Home deep cleaning",
                "Kitchen cleaning",
                "Bathroom cleaning",
                "Sofa cleaning"
            )
        )

        "Painting" -> ServiceInfo(
            description = "Professional painting services for interiors and exteriors.",
            price = "Starting from ₹999",
            features = listOf(
                "Wall painting",
                "Texture painting",
                "Interior painting",
                "Exterior painting"
            )
        )

        "Carpenter" -> ServiceInfo(
            description = "Skilled carpenters for furniture repair and installation.",
            price = "Starting from ₹299",
            features = listOf(
                "Furniture repair",
                "Door installation",
                "Cupboard fixing",
                "Wood polishing"
            )
        )

        else -> ServiceInfo(
            description = "Professional service at your doorstep.",
            price = "Contact for price",
            features = listOf("Professional service")
        )
    }

    Scaffold(

        topBar = {
            TopAppBar(
                title = { Text(displayServiceName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {

            Text(
                text = displayServiceName,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = serviceData.description,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = serviceData.price,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "What's Included",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            serviceData.features.forEach { feature ->

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Default.Build,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(feature)

                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {

                    navController.navigate(
                        Routes.BOOKING + "/$displayServiceName"
                    )

                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text("Book Service")

            }

        }

    }
}