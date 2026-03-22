package com.example.nidhi.ui.screens.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.nidhi.data.model.ServiceCategory
import com.example.nidhi.navigation.Routes
import com.example.nidhi.viewmodel.BookingResult
import com.example.nidhi.viewmodel.BookingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    serviceName: String,
    navController: NavController
) {
    val viewModel: BookingViewModel = viewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val bookingResult by viewModel.bookingResult.collectAsState()

    val displayName = serviceName.replace("_", " ")

    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Date picker
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    var selectedDate by remember { mutableStateOf("") }

    // Time picker
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(is24Hour = false)
    var selectedTime by remember { mutableStateOf("") }

    // Handle booking result
    LaunchedEffect(bookingResult) {
        when (val result = bookingResult) {
            is BookingResult.Success -> {
                viewModel.clearBookingResult()
                navController.navigate(
                    Routes.PAYMENT + "/${result.bookingId}/${result.serviceName.replace(" ", "_")}/${result.amount.toInt()}"
                )
            }
            is BookingResult.Failure -> {
                errorMessage = result.message
                viewModel.clearBookingResult()
            }
            null -> Unit
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        selectedDate = format.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val hour = timePickerState.hour
                    val minute = timePickerState.minute
                    val amPm = if (hour < 12) "AM" else "PM"
                    val displayHour = when {
                        hour == 0 -> 12
                        hour > 12 -> hour - 12
                        else -> hour
                    }
                    selectedTime = "%d:%02d %s".format(displayHour, minute, amPm)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book $displayName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Service Details",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it; errorMessage = "" },
                label = { Text("Service Address") },
                placeholder = { Text("Enter your full address") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage.contains("address", ignoreCase = true),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = {},
                    label = { Text("Date") },
                    placeholder = { Text("Select date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Date")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    isError = errorMessage.contains("date", ignoreCase = true),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = {},
                    label = { Text("Time") },
                    placeholder = { Text("Select time") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Pick Time")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    isError = errorMessage.contains("time", ignoreCase = true),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Additional Notes (Optional)") },
                placeholder = { Text("Any specific requirements...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            // Pricing info
            val price = ServiceCategory.fromDisplayName(displayName)?.startingPrice ?: 299
            Text(
                text = "Starting from ₹$price",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    when {
                        address.isEmpty() -> errorMessage = "Please enter your address"
                        selectedDate.isEmpty() -> errorMessage = "Please select a date"
                        selectedTime.isEmpty() -> errorMessage = "Please select a time"
                        else -> {
                            viewModel.createBooking(
                                serviceName = displayName,
                                address = address,
                                scheduledDate = selectedDate,
                                scheduledTime = selectedTime,
                                amount = price.toDouble(),
                                notes = notes
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Proceed to Payment",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
