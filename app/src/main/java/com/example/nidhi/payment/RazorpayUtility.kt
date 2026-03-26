package com.example.nidhi.payment

import com.example.nidhi.data.model.PaymentMethod
import org.json.JSONObject

/** Helper functions for building Razorpay checkout options and mapping error codes. */
object RazorpayUtility {

    /**
     * Builds the [JSONObject] that Razorpay's [com.razorpay.Checkout.open] expects.
     *
     * @param amount       Amount in INR (converted to paise internally).
     * @param serviceName  Name of the booked service (used in the checkout description).
     * @param userName     Display name shown in the prefill section.
     * @param userEmail    Email shown in the prefill section (may be empty).
     * @param userPhone    Phone number shown in the prefill section (may be empty).
     * @param method       Preferred payment method used to suggest a default tab.
     */
    fun buildCheckoutOptions(
        amount: Double,
        serviceName: String,
        userName: String = "",
        userEmail: String = "",
        userPhone: String = "",
        method: PaymentMethod = PaymentMethod.UPI
    ): JSONObject {
        val amountInPaise = (amount * RazorpayConfig.AMOUNT_MULTIPLIER).toLong()

        val prefill = JSONObject().apply {
            if (userName.isNotBlank()) put("name", userName)
            if (userEmail.isNotBlank()) put("email", userEmail)
            if (userPhone.isNotBlank()) put("contact", userPhone)
        }

        return JSONObject().apply {
            put("name", RazorpayConfig.COMPANY_NAME)
            put(
                "description",
                "${RazorpayConfig.DESCRIPTION_PREFIX}${serviceName.replace("_", " ")}"
            )
            put("currency", RazorpayConfig.CURRENCY)
            put("amount", amountInPaise)
            put("prefill", prefill)
            put("method", method.razorpayKey)
        }
    }

    /**
     * Returns a user-friendly error message for the given Razorpay error code.
     *
     * Standard codes:
     *  - 0 → payment cancelled by user
     *  - 1 → invalid/missing options
     *  - 2 → network failure
     */
    fun getErrorMessage(errorCode: Int, description: String?): String = when {
        errorCode == 0 -> "Payment was cancelled"
        !description.isNullOrBlank() -> description
        errorCode == 2 -> "Network error. Please check your connection and try again"
        else -> "Payment failed. Please try again"
    }
}
