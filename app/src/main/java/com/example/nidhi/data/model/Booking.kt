package com.example.nidhi.data.model

data class Booking(
    val serviceName: String = "",
    val address: String = "",
    val userId: String = "",
    val status: String = "pending",
    val timestamp: Long = System.currentTimeMillis()
)