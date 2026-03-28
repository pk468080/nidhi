package com.example.nidhi.payment

import com.example.nidhi.BuildConfig

/**
 * Razorpay API configuration.
 *
 * The Key ID is injected at build time from `local.properties` (which is excluded
 * from version control). See the `local.properties.example` file at the repo root
 * for the required property name.
 *
 * Use a "rzp_test_..." key for development and a "rzp_live_..." key for production.
 *
 * IMPORTANT: Never commit real API keys to source control.
 */
object RazorpayConfig {
    /** Razorpay Key ID – read from BuildConfig, which is populated from local.properties. */
    val KEY_ID: String get() = BuildConfig.RAZORPAY_KEY_ID

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
