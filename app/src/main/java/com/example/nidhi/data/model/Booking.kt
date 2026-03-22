package com.example.nidhi.data.model

data class Booking(
    val bookingId: String = "",
    val serviceName: String = "",
    val address: String = "",
    val userId: String = "",
    val status: String = "pending",
    val providerName: String = "",
    val providerPhone: String = "",
    val providerRating: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)