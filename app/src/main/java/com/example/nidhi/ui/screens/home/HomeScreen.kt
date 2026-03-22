package com.example.nidhi.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nidhi.data.model.Service
import com.example.nidhi.navigation.Routes
import com.example.nidhi.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {

    val viewModel: AuthViewModel = viewModel()

    val services = listOf(
        Service("AC Repair", Icons.Default.AcUnit),
        Service("Plumber", Icons.Default.Plumbing),
        Service("Electrician", Icons.Default.ElectricalServices),
        Service("Cleaning", Icons.Default.CleaningServices),
        Service("Painting", Icons.Default.FormatPaint),
        Service("Carpenter", Icons.Default.Handyman)
    )

    Scaffold(

        topBar = {

            TopAppBar(

                title = { Text("Nidhi Services") },

                actions = {

                    TextButton(

                        onClick = {

                            viewModel.logout()

                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0)
                            }

                        }

                    ) {
                        Text("Logout")
                    }

                }

            )

        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)

        ) {

            /* ---------------- HEADER ---------------- */

            Box(

                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF1976D2),
                                Color(0xFF42A5F5)
                            )
                        ),
                        shape = RoundedCornerShape(
                            bottomStart = 28.dp,
                            bottomEnd = 28.dp
                        )
                    )
                    .padding(20.dp)

            ) {

                Column {

                    Text(
                        text = "Hello, User 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "What service do you need today?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    /* Search */

                    OutlinedTextField(

                        value = "",
                        onValueChange = {},

                        readOnly = true,

                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate(Routes.SEARCH)
                            },

                        placeholder = {
                            Text("Search for services...")
                        },

                        leadingIcon = {
                            Icon(Icons.Default.Search, null)
                        },

                        shape = RoundedCornerShape(20.dp)

                    )

                }

            }

            Spacer(modifier = Modifier.height(20.dp))

            /* ---------------- SERVICES TITLE ---------------- */

            Text(

                text = "Our Services",

                style = MaterialTheme.typography.titleLarge,

                modifier = Modifier.padding(horizontal = 16.dp)

            )

            Spacer(modifier = Modifier.height(12.dp))

            /* ---------------- SERVICES GRID ---------------- */

            LazyVerticalGrid(

                columns = GridCells.Fixed(2),

                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(320.dp),

                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)

            ) {

                items(services) { service ->

                    ServiceCard(service, navController)

                }

            }

            Spacer(modifier = Modifier.height(24.dp))

            /* ---------------- POPULAR SERVICES ---------------- */

            Text(

                text = "Popular This Week",

                style = MaterialTheme.typography.titleLarge,

                modifier = Modifier.padding(horizontal = 16.dp)

            )

            Spacer(modifier = Modifier.height(12.dp))

            PopularServiceCard(
                title = "AC Service & Repair",
                price = "Starting from ₹299",
                icon = Icons.Default.AcUnit,
                navController = navController,
                route = "AC_Repair"
            )

            PopularServiceCard(
                title = "Deep Cleaning",
                price = "Starting from ₹499",
                icon = Icons.Default.CleaningServices,
                navController = navController,
                route = "Cleaning"
            )

        }

    }

}

@Composable
fun ServiceCard(service: Service, navController: NavController) {

    Card(

        shape = RoundedCornerShape(16.dp),

        elevation = CardDefaults.cardElevation(4.dp),

        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable {

                navController.navigate(
                    Routes.SERVICE_DETAILS + "/${service.name.replace(" ", "_")}"
                )

            }

    ) {

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center

        ) {

            Box(

                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = Color(0xFFE3F2FD),
                        shape = CircleShape
                    ),

                contentAlignment = Alignment.Center

            ) {

                Icon(
                    imageVector = service.icon,
                    contentDescription = service.name,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF1976D2)
                )

            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = service.name,
                style = MaterialTheme.typography.titleMedium
            )

        }

    }

}

@Composable
fun PopularServiceCard(
    title: String,
    price: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    navController: NavController,
    route: String
) {

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable {

                navController.navigate(
                    Routes.SERVICE_DETAILS + "/$route"
                )

            },

        shape = RoundedCornerShape(14.dp),

        elevation = CardDefaults.cardElevation(3.dp)

    ) {

        Row(

            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),

            verticalAlignment = Alignment.CenterVertically

        ) {

            Box(

                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(10.dp)
                    ),

                contentAlignment = Alignment.Center

            ) {

                Icon(icon, null)

            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = price,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

            }

        }

    }

}