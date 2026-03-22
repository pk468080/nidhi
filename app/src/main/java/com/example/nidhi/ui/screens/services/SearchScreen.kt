package com.example.nidhi.ui.screens.services

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nidhi.navigation.Routes
import com.example.nidhi.ui.screens.home.allServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {

    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val filtered = if (query.isBlank()) allServices
    else allServices.filter { service ->
        service.name.contains(query, ignoreCase = true) ||
            service.category.contains(query, ignoreCase = true) ||
            service.description.contains(query, ignoreCase = true)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search services…") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp)
        ) {
            items(filtered) { service ->
                ListItem(
                    headlineContent = { Text(service.name) },
                    supportingContent = {
                        if (service.basePrice > 0) {
                            Text("From ₹${service.basePrice} · ${service.duration}")
                        }
                    },
                    trailingContent = {
                        if (service.rating > 0f) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                                Text(" ${service.rating}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    leadingContent = {
                        Icon(service.icon, contentDescription = service.name)
                    },
                    modifier = Modifier.clickable {
                        navController.navigate(
                            Routes.SERVICE_DETAILS + "/${service.name.replace(" ", "_")}"
                        )
                    }
                )
                HorizontalDivider()
            }

            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No services found for \"$query\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
