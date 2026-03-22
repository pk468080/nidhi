package com.example.nidhi.data.model

enum class ServiceCategory(val displayName: String, val startingPrice: Int) {
    AC_REPAIR("AC Repair", 299),
    PLUMBER("Plumber", 199),
    ELECTRICIAN("Electrician", 249),
    CLEANING("Cleaning", 399),
    PAINTING("Painting", 999),
    CARPENTER("Carpenter", 299);

    companion object {
        fun fromDisplayName(name: String): ServiceCategory? =
            values().find { it.displayName.equals(name, ignoreCase = true) }
    }
}
