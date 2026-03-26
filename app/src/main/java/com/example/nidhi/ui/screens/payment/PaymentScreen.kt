package com.example.nidhi.ui.screens.payment

import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.nidhi.data.model.PaymentMethod
import com.example.nidhi.navigation.Routes
import com.example.nidhi.payment.RazorpayConfig
import com.example.nidhi.ui.theme.AppSpacing
import com.example.nidhi.viewmodel.PaymentResult
import com.example.nidhi.viewmodel.PaymentViewModel
import com.razorpay.Checkout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    bookingId: String,
    serviceName: String,
    amount: Double,
    navController: NavController
) {
    val viewModel: PaymentViewModel = viewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val paymentResult by viewModel.paymentResult.collectAsState()
    val checkoutOptions by viewModel.checkoutOptions.collectAsState()

    val context = LocalContext.current
    val checkout = remember { Checkout() }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI) }
    var errorMessage by remember { mutableStateOf("") }

    // Launch Razorpay Checkout when the ViewModel emits checkout options.
    LaunchedEffect(checkoutOptions) {
        val options = checkoutOptions ?: return@LaunchedEffect
        val activity = context as? ComponentActivity
        if (activity == null) {
            viewModel.onCheckoutFailed()
            return@LaunchedEffect
        }
        viewModel.onCheckoutLaunched()
        checkout.setKeyID(RazorpayConfig.KEY_ID)
        checkout.open(activity, options)
    }

    LaunchedEffect(paymentResult) {
        when (val result = paymentResult) {
            is PaymentResult.Success -> {
                viewModel.clearPaymentResult()
                navController.navigate(Routes.BOOKING_DETAILS + "/$bookingId") {
                    popUpTo(Routes.HOME)
                }
            }
            is PaymentResult.Failure -> {
                errorMessage = result.message
                viewModel.clearPaymentResult()
            }
            null -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment") },
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

            // Order summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(AppSpacing.large)) {
                    Text(
                        text = "Order Summary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.medium))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = serviceName.replace("_", " "),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "₹${"%.0f".format(amount)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.extraSmall))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Convenience Fee",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "₹0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = AppSpacing.small))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "₹${"%.0f".format(amount)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Text(
                text = "Select Payment Method",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            PaymentMethod.values().forEach { method ->
                PaymentMethodCard(
                    method = method,
                    isSelected = selectedMethod == method,
                    onSelect = { selectedMethod = method }
                )
            }

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
                    errorMessage = ""
                    viewModel.processPayment(
                        bookingId = bookingId,
                        serviceName = serviceName,
                        amount = amount,
                        method = selectedMethod
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
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
                        text = "Pay ₹${"%.0f".format(amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Secure payment note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(AppSpacing.extraSmall))
                Text(
                    text = "Payments powered by Razorpay",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    method: PaymentMethod,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val (icon, tint) = when (method) {
        PaymentMethod.UPI -> Pair(Icons.Default.AccountBalance, MaterialTheme.colorScheme.primary)
        PaymentMethod.CARD -> Pair(Icons.Default.CreditCard, MaterialTheme.colorScheme.tertiary)
        PaymentMethod.WALLET -> Pair(Icons.Default.AccountBalanceWallet, MaterialTheme.colorScheme.secondary)
        PaymentMethod.NET_BANKING -> Pair(Icons.Default.Language, MaterialTheme.colorScheme.error)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = MaterialTheme.shapes.medium,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(AppSpacing.default)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = method.displayName,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.default))
            Text(
                text = method.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            RadioButton(selected = isSelected, onClick = onSelect)
        }
    }
}
