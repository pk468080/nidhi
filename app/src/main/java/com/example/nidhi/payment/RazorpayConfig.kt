package com.example.nidhi.payment

import com.example.nidhi.BuildConfig

/**
 * Razorpay API configuration.
 *
 * Set your Razorpay Key ID in `local.properties` (project root, already git-ignored):
 *
 *   razorpay.keyId=rzp_test_xxxxxxxxxxxx
 *
 * Use a "rzp_test_..." key for development and a "rzp_live_..." key for production.
 * The value is injected at build time via BuildConfig.RAZORPAY_KEY_ID – no source
 * changes are needed to swap keys between environments.
 *
 * For CI/CD, set the Gradle property `razorpay.keyId` via:
 *   - A `local.properties` file generated from a secret
 *   - Or by passing `-Prazorpay.keyId=...` on the Gradle command line
 *
 * IMPORTANT: Do NOT commit live keys to source control.
 */
object RazorpayConfig {
    /** Razorpay Key ID – set via `razorpay.keyId` in local.properties. */
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
