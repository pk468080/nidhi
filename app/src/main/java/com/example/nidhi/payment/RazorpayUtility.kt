package com.example.nidhi.payment

import org.json.JSONObject

/** Helper functions for building Razorpay checkout options and mapping error codes. */
object RazorpayUtility {

    /**
     * Builds the [JSONObject] that Razorpay's [com.razorpay.Checkout.open] expects.
     *
     * @param amount       Amount in INR (converted to paise internally).
     * @param serviceName  Name of the booked service (used in the checkout description).
     * @param bookingId    Booking document ID, passed as a note so the server-side webhook
     *                     can link the payment back to the correct booking.
     * @param userId       Firebase UID of the paying user, passed as a note for the webhook.
     * @param userName     Display name shown in the prefill section.
     * @param userEmail    Email shown in the prefill section (may be empty).
     * @param userPhone    Phone number shown in the prefill section (may be empty).
     */
    fun buildCheckoutOptions(
        amount: Double,
        serviceName: String,
        bookingId: String,
        userId: String,
        userName: String = "",
        userEmail: String = "",
        userPhone: String = ""
    ): JSONObject {
        val amountInPaise = (amount * RazorpayConfig.AMOUNT_MULTIPLIER).toLong()

        val prefill = JSONObject().apply {
            if (userName.isNotBlank()) put("name", userName)
            if (userEmail.isNotBlank()) put("email", userEmail)
            if (userPhone.isNotBlank()) put("contact", userPhone)
        }

        val notes = JSONObject().apply {
            put("bookingId", bookingId)
            put("userId", userId)
        }

        return JSONObject().apply {
            put("name", RazorpayConfig.COMPANY_NAME)
            put(
                "description",
                "${RazorpayConfig.DESCRIPTION_PREFIX}$serviceName"
            )
            put("currency", RazorpayConfig.CURRENCY)
            put("amount", amountInPaise)
            put("prefill", prefill)
            put("notes", notes)
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
