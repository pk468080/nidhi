package com.example.nidhi.viewmodel

import androidx.lifecycle.ViewModel
import com.example.nidhi.data.model.Payment
import com.example.nidhi.data.model.PaymentMethod
import com.example.nidhi.data.model.PaymentStatus
import com.example.nidhi.data.repository.PaymentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class PaymentViewModel : ViewModel() {

    private val repository = PaymentRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _transactions = MutableStateFlow<List<Payment>>(emptyList())
    val transactions: StateFlow<List<Payment>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _paymentResult = MutableStateFlow<PaymentResult?>(null)
    val paymentResult: StateFlow<PaymentResult?> = _paymentResult.asStateFlow()

    fun loadTransactions() {
        val userId = auth.currentUser?.uid ?: return
        repository.getUserTransactions(userId) { payments ->
            _transactions.value = payments
        }
    }

    fun processPayment(
        bookingId: String,
        serviceName: String,
        amount: Double,
        method: PaymentMethod
    ) {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        // NOTE: Replace this block with the actual Razorpay Checkout call when
        // integrating the Razorpay SDK (com.razorpay:checkout).  The payment
        // object below is persisted to Firestore to record the transaction.
        val payment = Payment(
            bookingId = bookingId,
            userId = userId,
            serviceName = serviceName,
            amount = amount,
            method = method.value,
            status = PaymentStatus.PAID.value,
            transactionId = "TXN_${UUID.randomUUID().toString().take(12).uppercase()}"
        )

        repository.savePayment(payment) { success, _ ->
            if (success) {
                // Update the corresponding booking's payment status
                firestore.collection("bookings").document(bookingId)
                    .update("paymentStatus", PaymentStatus.PAID.value)
                    .addOnCompleteListener {
                        _isLoading.value = false
                        _paymentResult.value = PaymentResult.Success(payment.transactionId)
                    }
            } else {
                _isLoading.value = false
                _paymentResult.value = PaymentResult.Failure("Payment failed. Please try again.")
            }
        }
    }

    fun clearPaymentResult() {
        _paymentResult.value = null
    }
}

sealed class PaymentResult {
    data class Success(val transactionId: String) : PaymentResult()
    data class Failure(val message: String) : PaymentResult()
}
