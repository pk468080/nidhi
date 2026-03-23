package com.example.nidhi.data.model

data class Review(
    val reviewId: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val providerId: String = "",
    val serviceName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
