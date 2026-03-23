package com.example.nidhi.data.model

data class Provider(
    val providerId: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val category: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val isAvailable: Boolean = true,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
