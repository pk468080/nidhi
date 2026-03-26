package com.example.nidhi.ui.screens.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.nidhi.data.model.Booking
import com.example.nidhi.data.model.BookingStatus
import com.example.nidhi.navigation.Routes
import com.example.nidhi.ui.theme.AppSpacing
import com.example.nidhi.ui.theme.Error
import com.example.nidhi.ui.theme.Info
import com.example.nidhi.ui.theme.Success
import com.example.nidhi.ui.theme.Warning
import com.example.nidhi.viewmodel.BookingListViewModel

@Composable
fun BookingsScreen(navController: NavController) {

    val viewModel: BookingListViewModel = viewModel()
    val state by viewModel.paginationState.collectAsState()
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.large)
    ) {

        Text(
            text = "My Bookings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(AppSpacing.large))

        if (!state.isLoading && state.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(AppSpacing.xxxLarge),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No bookings yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Trigger next page load when scrolled within 5 items of the list end.
            val shouldLoadNext by remember {
                derivedStateOf {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    lastVisible >= state.items.size - 5
                }
            }
            LaunchedEffect(shouldLoadNext) {
                if (shouldLoadNext) viewModel.loadNextPage()
            }

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                items(state.items) { booking ->
                    BookingListCard(booking = booking, navController = navController)
                }

                // Bottom loading indicator while next page is being fetched.
                if (state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.default),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // End-of-list indicator once all pages are loaded.
                if (!state.hasMore && state.items.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.default),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No more bookings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingListCard(booking: Booking, navController: NavController) {
    val statusColor = when (booking.status) {
        BookingStatus.PENDING.value -> Warning
        BookingStatus.ACCEPTED.value, BookingStatus.ON_THE_WAY.value -> Info
        BookingStatus.ARRIVED.value, BookingStatus.COMPLETED.value -> Success
        BookingStatus.REJECTED.value, BookingStatus.CANCELLED.value -> Error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (booking.bookingId.isNotBlank()) {
                    navController.navigate(Routes.BOOKING_DETAILS + "/${booking.bookingId}")
                }
            },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = AppSpacing.extraSmall),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(AppSpacing.default)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = booking.serviceName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (booking.amount > 0) {
                    Text(
                        text = "₹${"%.0f".format(booking.amount)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.extraSmall))

            if (booking.address.isNotEmpty()) {
                Text(
                    text = booking.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (booking.scheduledDate.isNotEmpty()) {
                Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                Text(
                    text = "${booking.scheduledDate}${if (booking.scheduledTime.isNotEmpty()) " at ${booking.scheduledTime}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.small))

            Surface(
                color = statusColor.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.large
            ) {
                val statusLabel = BookingStatus.values()
                    .find { it.value == booking.status }?.displayName
                    ?: booking.status.replaceFirstChar { it.uppercase() }
                Text(
                    text = statusLabel,
                    modifier = Modifier.padding(horizontal = AppSpacing.small, vertical = AppSpacing.extraSmall),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = statusColor
                )
            }
        }
    }
}
