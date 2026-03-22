package com.example.nidhi.ui.screens.payment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.nidhi.data.model.Payment
import com.example.nidhi.data.model.PaymentStatus
import com.example.nidhi.viewmodel.PaymentViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(navController: NavController) {

    val viewModel: PaymentViewModel = viewModel()
    val transactions by viewModel.transactions.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTransactions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transactions yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(transactions) { payment ->
                    TransactionCard(payment)
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(payment: Payment) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val date = dateFormat.format(Date(payment.timestamp))
    val isSuccess = payment.status == PaymentStatus.PAID.value

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.serviceName.replace("_", " "),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = payment.method.uppercase().replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (payment.transactionId.isNotEmpty()) {
                    Text(
                        text = "ID: ${payment.transactionId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${"%.0f".format(payment.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isSuccess) "Paid"
                    else payment.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}
