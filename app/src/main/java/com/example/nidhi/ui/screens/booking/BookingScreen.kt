package com.example.nidhi.ui.screens.booking

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.nidhi.R
import com.example.nidhi.data.model.ServiceCategory
import com.example.nidhi.navigation.Routes
import com.example.nidhi.ui.theme.AppSpacing
import com.example.nidhi.viewmodel.BookingResult
import com.example.nidhi.viewmodel.BookingViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val bookingResult by viewModel.bookingResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val displayName = serviceName.replace("_", " ")

    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isFetchingAddress by remember { mutableStateOf(false) }

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

    fun fetchAndFillCurrentAddress() {
        if (isFetchingAddress) return

        isFetchingAddress = true
        errorMessage = ""
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.booking_location_fetching))
        }

        getCurrentAddressText(
            context = context,
            onSuccess = { detectedAddress ->
                isFetchingAddress = false
                address = detectedAddress
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.booking_location_autofilled))
                }
            },
            onFailure = {
                isFetchingAddress = false
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.booking_location_fetch_failed))
                }
            },
            onLocationUnavailable = {
                isFetchingAddress = false
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.booking_location_unavailable))
                }
            }
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionResult ->
        val granted = permissionResult[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissionResult[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            fetchAndFillCurrentAddress()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.booking_location_permission_denied))
            }
        }
    }

    fun triggerAddressAutofill() {
        if (hasLocationPermission(context)) {
            fetchAndFillCurrentAddress()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

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

    // Auto-fill when screen opens if permission already granted.
    LaunchedEffect(Unit) {
        if (address.isBlank() && hasLocationPermission(context)) {
            fetchAndFillCurrentAddress()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                .padding(AppSpacing.default)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {

            Text(
                text = "Service Details",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it; errorMessage = "" },
                label = { Text(stringResource(R.string.booking_service_address_label)) },
                placeholder = { Text(stringResource(R.string.booking_service_address_placeholder)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.booking_location_icon_content_description)
                    )
                },
                trailingIcon = {
                    if (isFetchingAddress) {
                        CircularProgressIndicator(modifier = Modifier.size(AppSpacing.large), strokeWidth = AppSpacing.extraSmall / 2)
                    } else {
                        IconButton(onClick = { triggerAddressAutofill() }) {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = stringResource(R.string.booking_use_current_location)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage.contains("address", ignoreCase = true),
                shape = MaterialTheme.shapes.medium
            )

            TextButton(
                onClick = { triggerAddressAutofill() },
                enabled = !isFetchingAddress,
                contentPadding = PaddingValues(AppSpacing.extraSmall / 4)
            ) {
                Text(stringResource(R.string.booking_use_current_location))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
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
                    shape = MaterialTheme.shapes.medium
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
                    shape = MaterialTheme.shapes.medium
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Additional Notes (Optional)") },
                placeholder = { Text("Any specific requirements...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = MaterialTheme.shapes.medium
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

            Spacer(modifier = Modifier.height(AppSpacing.small))

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
                    .height(AppSpacing.xxxLarge + AppSpacing.small),
                enabled = !isLoading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppSpacing.extraLarge),
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

private fun hasLocationPermission(context: Context): Boolean {
    val fineLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fineLocationGranted || coarseLocationGranted
}

@SuppressLint("MissingPermission")
private fun getCurrentAddressText(
    context: Context,
    onSuccess: (String) -> Unit,
    onFailure: () -> Unit,
    onLocationUnavailable: () -> Unit
) {
    if (!hasLocationPermission(context)) {
        onLocationUnavailable()
        return
    }

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location == null) {
                    onLocationUnavailable()
                    return@addOnSuccessListener
                }

                resolveAddressFromCoordinates(
                    context = context,
                    location = location,
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            }
            .addOnFailureListener {
                onLocationUnavailable()
            }
    } catch (_: SecurityException) {
        onLocationUnavailable()
    }
}

private fun resolveAddressFromCoordinates(
    context: Context,
    location: Location,
    onSuccess: (String) -> Unit,
    onFailure: () -> Unit
) {
    if (!Geocoder.isPresent()) {
        onFailure()
        return
    }

    val geocoder = Geocoder(context, Locale.getDefault())

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        geocoder.getFromLocation(location.latitude, location.longitude, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                val fullAddress = addresses.firstOrNull()?.toDisplayAddress()
                if (fullAddress.isNullOrBlank()) onFailure() else onSuccess(fullAddress)
            }

            override fun onError(errorMessage: String?) {
                onFailure()
            }
        })
    } else {
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        val fullAddress = addresses?.firstOrNull()?.toDisplayAddress()
        if (fullAddress.isNullOrBlank()) onFailure() else onSuccess(fullAddress)
    }
}

private fun Address.toDisplayAddress(): String? {
    return getAddressLine(0)
        ?: listOfNotNull(featureName, subLocality, locality, adminArea, countryName)
            .joinToString(", ")
            .ifBlank { null }
}
