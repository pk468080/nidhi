package com.example.nidhi.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = UserRole.CUSTOMER.value,
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class UserRole(val value: String) {
    CUSTOMER("customer"),
    PROVIDER("provider")
}

