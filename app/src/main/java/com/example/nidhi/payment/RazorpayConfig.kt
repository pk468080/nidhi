package com.example.nidhi.payment

/**
 * Razorpay API configuration.
 *
 * Replace KEY_ID with your actual Key ID from https://dashboard.razorpay.com/app/keys.
 * Use a "rzp_test_..." key for development and a "rzp_live_..." key for production.
 *
 * IMPORTANT: Do NOT commit live keys to source control. Inject them via a secure
 * mechanism such as a secrets manager, CI/CD environment variables, or an encrypted
 * local.properties file that is excluded from version control.
 */
object RazorpayConfig {
    /** Razorpay Key ID – replace with your own key before testing. */
    const val KEY_ID = "rzp_test_YOUR_KEY_ID"

    /** ISO 4217 currency code used for all transactions. */
    const val CURRENCY = "INR"

    /** Merchant name displayed in the Razorpay checkout sheet. */
    const val COMPANY_NAME = "Nidhi Services"

    /** Prefix prepended to the service name in the checkout description. */
    const val DESCRIPTION_PREFIX = "Payment for "

    /**
     * Razorpay expects the amount in the smallest currency unit (paise for INR).
     * Multiply the rupee amount by this constant before sending to the SDK.
     */
    const val AMOUNT_MULTIPLIER = 100
}
