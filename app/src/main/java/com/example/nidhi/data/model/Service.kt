package com.example.nidhi.data.model

import androidx.compose.ui.graphics.vector.ImageVector

data class Service(
    val name: String,
    val icon: ImageVector,
    val description: String = "",
    val basePrice: Int = 0,
    val duration: String = "",
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val category: String = ""
)