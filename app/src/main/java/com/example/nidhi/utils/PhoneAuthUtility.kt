package com.example.nidhi.utils

/**
 * Format a raw phone string to E.164 format (+91XXXXXXXXXX) for Indian numbers.
 * Returns null if the input cannot be converted to a valid Indian phone number.
 *
 * Supported inputs:
 *  - 10-digit number (e.g. "9876543210")      → "+919876543210"
 *  - "91" prefixed 12-digit number             → "+919876543210"
 *  - Already formatted "+91" E.164 number      → unchanged
 */
fun formatIndianPhoneNumber(raw: String): String? {
    val cleaned = raw.trim().replace(" ", "").replace("-", "")
    return when {
        cleaned.startsWith("+91") && cleaned.length == 13 -> cleaned
        cleaned.startsWith("91") && cleaned.length == 12 -> "+$cleaned"
        cleaned.length == 10 && cleaned.all { it.isDigit() } -> "+91$cleaned"
        else -> null
    }
}
