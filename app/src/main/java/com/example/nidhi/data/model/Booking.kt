package com.example.nidhi.data.model

data class Booking(
    val bookingId: String = "",
    val serviceName: String = "",
    val address: String = "",
    val userId: String = "",
    val status: String = BookingStatus.PENDING.value,
    val providerId: String = "",
    val providerName: String = "",
    val providerPhone: String = "",
    val providerRating: Double = 0.0,
    val scheduledDate: String = "",
    val scheduledTime: String = "",
    val amount: Double = 0.0,
    val paymentStatus: String = PaymentStatus.PENDING.value,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class BookingStatus(val value: String, val displayName: String) {
    PENDING("pending", "Pending"),
    ACCEPTED("accepted", "Accepted"),
    REJECTED("rejected", "Rejected"),
    ON_THE_WAY("on_the_way", "Provider On the Way"),
    ARRIVED("arrived", "Provider Arrived"),
    COMPLETED("completed", "Completed"),
    CANCELLED("cancelled", "Cancelled")
}

enum class PaymentStatus(val value: String, val displayName: String) {
    PENDING("pending", "Payment Pending"),
    PAID("paid", "Paid"),
    FAILED("failed", "Payment Failed"),
    REFUNDED("refunded", "Refunded")
}