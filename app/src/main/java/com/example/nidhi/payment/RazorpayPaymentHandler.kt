package com.example.nidhi.payment

/**
 * Singleton bridge that connects Razorpay payment callbacks received in [MainActivity]
 * to the [com.example.nidhi.viewmodel.PaymentViewModel].
 *
 * Usage:
 *  - Call [registerCallbacks] from [PaymentViewModel.init] to listen for results.
 *  - Call [unregisterCallbacks] from [PaymentViewModel.onCleared] to avoid leaks.
 *  - [MainActivity] forwards [onPaymentSuccess] / [onPaymentError] to [handleSuccess] /
 *    [handleError].
 */
object RazorpayPaymentHandler {

    @Volatile private var onSuccess: ((String) -> Unit)? = null
    @Volatile private var onError: ((Int, String?) -> Unit)? = null

    /** Register ViewModel-level callbacks for payment results. */
    fun registerCallbacks(
        onSuccess: (String) -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        this.onSuccess = onSuccess
        this.onError = onError
    }

    /** Unregister callbacks (call from ViewModel.onCleared to prevent leaks). */
    fun unregisterCallbacks() {
        onSuccess = null
        onError = null
    }

    /** Invoked by MainActivity when Razorpay reports a successful payment. */
    fun handleSuccess(razorpayPaymentId: String) {
        onSuccess?.invoke(razorpayPaymentId)
    }

    /** Invoked by MainActivity when Razorpay reports a payment failure or cancellation. */
    fun handleError(errorCode: Int, errorDescription: String?) {
        onError?.invoke(errorCode, errorDescription)
    }
}
