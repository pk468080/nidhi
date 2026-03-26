package com.example.nidhi.viewmodel

import androidx.lifecycle.ViewModel
import com.example.nidhi.data.model.Payment
import com.example.nidhi.data.model.PaymentMethod
import com.example.nidhi.data.model.PaymentStatus
import com.example.nidhi.data.repository.PaymentRepository
import com.example.nidhi.payment.RazorpayPaymentHandler
import com.example.nidhi.payment.RazorpayUtility
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

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

    /**
     * Emits the Razorpay checkout options when the user taps "Pay".
     * PaymentScreen observes this and calls Checkout.open() on the Activity.
     * Reset to null by calling [onCheckoutLaunched] after the checkout is opened.
     */
    private val _checkoutOptions = MutableStateFlow<JSONObject?>(null)
    val checkoutOptions: StateFlow<JSONObject?> = _checkoutOptions.asStateFlow()

    // Pending payment details stored while Razorpay checkout is in progress.
    private var pendingPayment: Payment? = null
    private var pendingBookingId: String? = null

    init {
        RazorpayPaymentHandler.registerCallbacks(
            onSuccess = { paymentId -> onRazorpaySuccess(paymentId) },
            onError = { code, description -> onRazorpayError(code, description) }
        )
    }

    override fun onCleared() {
        super.onCleared()
        RazorpayPaymentHandler.unregisterCallbacks()
    }

    fun loadTransactions() {
        val userId = auth.currentUser?.uid ?: return
        repository.getUserTransactions(userId) { payments ->
            _transactions.value = payments
        }
    }

    /**
     * Prepares and emits Razorpay checkout options.
     * The actual checkout is opened by [PaymentScreen] once it observes the emitted options.
     */
    fun processPayment(
        bookingId: String,
        serviceName: String,
        amount: Double,
        method: PaymentMethod
    ) {
        val user = auth.currentUser
        if (user == null) {
            _paymentResult.value = PaymentResult.Failure("Please log in again and retry payment")
            return
        }
        val userId = user.uid
        _isLoading.value = true

        pendingBookingId = bookingId
        pendingPayment = Payment(
            bookingId = bookingId,
            userId = userId,
            serviceName = serviceName,
            amount = amount,
            method = method.value,
            status = PaymentStatus.PENDING.value,
            transactionId = ""
        )

        val options = RazorpayUtility.buildCheckoutOptions(
            amount = amount,
            serviceName = serviceName,
            userName = user.displayName ?: "",
            userEmail = user.email ?: "",
            userPhone = user.phoneNumber ?: ""
        )
        _checkoutOptions.value = options
    }

    /** Called by PaymentScreen immediately after Checkout.open() to reset the trigger. */
    fun onCheckoutLaunched() {
        _checkoutOptions.value = null
    }

    /** Called when Checkout.open() throws before the Razorpay UI is shown. */
    fun onCheckoutLaunchFailed(message: String) {
        pendingPayment = null
        pendingBookingId = null
        _isLoading.value = false
        _paymentResult.value = PaymentResult.Failure(message)
    }

    /** Called indirectly via [RazorpayPaymentHandler] from MainActivity.onPaymentSuccess. */
    private fun onRazorpaySuccess(razorpayPaymentId: String) {
        val payment = pendingPayment?.copy(
            status = PaymentStatus.PAID.value,
            transactionId = razorpayPaymentId
        ) ?: return
        val bookingId = pendingBookingId ?: return

        pendingPayment = null
        pendingBookingId = null

        // TODO: For production, verify the Razorpay payment signature via a Cloud Function
        // (see: https://razorpay.com/docs/payments/webhooks/validate-webhook-signature/)
        // before marking the booking as PAID to prevent client-side manipulation.
        repository.savePayment(payment) { success, _ ->
            if (success) {
                firestore.collection("bookings").document(bookingId)
                    .update("paymentStatus", PaymentStatus.PAID.value)
                    .addOnCompleteListener { task ->
                        _isLoading.value = false
                        _paymentResult.value = if (task.isSuccessful) {
                            PaymentResult.Success(razorpayPaymentId)
                        } else {
                            PaymentResult.Failure("Payment captured, but booking status sync failed. Please refresh.")
                        }
                    }
            } else {
                _isLoading.value = false
                _paymentResult.value =
                    PaymentResult.Failure("Failed to save payment. Please contact support.")
            }
        }
    }

    /** Called indirectly via [RazorpayPaymentHandler] from MainActivity.onPaymentError. */
    private fun onRazorpayError(errorCode: Int, errorDescription: String?) {
        pendingPayment = null
        pendingBookingId = null
        _isLoading.value = false
        _paymentResult.value =
            PaymentResult.Failure(RazorpayUtility.getErrorMessage(errorCode, errorDescription))
    }

    fun clearPaymentResult() {
        _paymentResult.value = null
    }
}

sealed class PaymentResult {
    data class Success(val transactionId: String) : PaymentResult()
    data class Failure(val message: String) : PaymentResult()
}
