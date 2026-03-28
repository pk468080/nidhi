package com.example.nidhi.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import com.example.nidhi.data.model.Payment
import com.example.nidhi.data.model.PaymentMethod
import com.example.nidhi.data.model.PaymentStatus
import com.example.nidhi.data.repository.PaymentRepository
import com.example.nidhi.payment.RazorpayPaymentHandler
import com.example.nidhi.payment.RazorpayUtility
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
     * True while waiting for the Razorpay webhook to confirm the payment server-side.
     * The UI should show a "Verifying payment…" message during this phase.
     */
    private val _awaitingWebhook = MutableStateFlow(false)
    val awaitingWebhook: StateFlow<Boolean> = _awaitingWebhook.asStateFlow()

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

    /** Firestore snapshot listener waiting for the webhook to mark the booking paid. */
    private var bookingListener: ListenerRegistration? = null

    /** Handler used to post a timeout if the webhook does not arrive within 2 minutes. */
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webhookTimeoutRunnable: Runnable? = null

    init {
        RazorpayPaymentHandler.registerCallbacks(
            onSuccess = { paymentId -> onRazorpaySuccess(paymentId) },
            onError = { code, description -> onRazorpayError(code, description) }
        )
    }

    override fun onCleared() {
        super.onCleared()
        RazorpayPaymentHandler.unregisterCallbacks()
        stopListeningForPaymentConfirmation()
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
            bookingId = bookingId,
            userId = userId,
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

    /**
     * Called indirectly via [RazorpayPaymentHandler] from MainActivity.onPaymentSuccess.
     *
     * The client does NOT update Firestore directly. Instead, it starts listening to the
     * booking document. The Razorpay webhook (server-side) will verify the signature and
     * mark the booking as paid. When the listener detects paymentStatus == "paid", the
     * UI is notified via [PaymentResult.Success].
     */
    private fun onRazorpaySuccess(razorpayPaymentId: String) {
        val bookingId = pendingBookingId ?: return
        pendingPayment = null
        pendingBookingId = null

        _awaitingWebhook.value = true
        listenForPaymentConfirmation(bookingId, razorpayPaymentId)
    }

    /**
     * Attaches a Firestore snapshot listener to the booking document.
     * Emits [PaymentResult.Success] once the server-side webhook marks
     * [paymentStatus] as "paid".
     * A 2-minute timeout is scheduled; if the webhook has not arrived by then,
     * a failure is emitted so the UI does not remain stuck indefinitely.
     */
    private fun listenForPaymentConfirmation(bookingId: String, razorpayPaymentId: String) {
        stopListeningForPaymentConfirmation()
        bookingListener = firestore.collection("bookings").document(bookingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    stopListeningForPaymentConfirmation()
                    _isLoading.value = false
                    _awaitingWebhook.value = false
                    _paymentResult.value =
                        PaymentResult.Failure("Payment verification failed. Please check your booking status.")
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val paymentStatus = snapshot.getString("paymentStatus")
                if (paymentStatus == PaymentStatus.PAID.value) {
                    stopListeningForPaymentConfirmation()
                    _isLoading.value = false
                    _awaitingWebhook.value = false
                    _paymentResult.value = PaymentResult.Success(razorpayPaymentId)
                }
            }

        // Schedule a 2-minute timeout in case the webhook never arrives.
        val timeoutRunnable = Runnable {
            stopListeningForPaymentConfirmation()
            _isLoading.value = false
            _awaitingWebhook.value = false
            _paymentResult.value = PaymentResult.Failure(
                "Payment confirmation is taking longer than expected. " +
                    "Please check your booking status or contact support."
            )
        }
        webhookTimeoutRunnable = timeoutRunnable
        mainHandler.postDelayed(timeoutRunnable, WEBHOOK_TIMEOUT_MS)
    }

    /** Removes the Firestore booking listener and cancels any pending timeout. */
    private fun stopListeningForPaymentConfirmation() {
        webhookTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        webhookTimeoutRunnable = null
        bookingListener?.remove()
        bookingListener = null
    }

    /** Called indirectly via [RazorpayPaymentHandler] from MainActivity.onPaymentError. */
    private fun onRazorpayError(errorCode: Int, errorDescription: String?) {
        pendingPayment = null
        pendingBookingId = null
        _isLoading.value = false
        _awaitingWebhook.value = false
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

private const val WEBHOOK_TIMEOUT_MS = 2 * 60 * 1000L // 2 minutes
