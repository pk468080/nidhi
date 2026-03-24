package com.example.nidhi.utils

import kotlin.math.roundToInt

/**
 * Calculates estimated time of arrival (ETA) from distance.
 *
 * Assumes an average city speed of 40 km/h and adds a 5-minute buffer for
 * traffic and minor delays.
 */
object ETACalculator {

    private const val AVERAGE_SPEED_KMH = 40.0
    private const val TRAFFIC_BUFFER_MINUTES = 5

    /**
     * Returns ETA in minutes for [distanceKm] kilometres.
     * Returns 0 when the provider has arrived (distance is 0 or negative).
     * Returns at least 1 minute for any positive distance so the display
     * never shows "0 min" while the provider is still en-route.
     */
    fun calculateEta(distanceKm: Double): Int {
        if (distanceKm <= 0) return 0
        val travelMinutes = (distanceKm / AVERAGE_SPEED_KMH * 60).roundToInt()
        return (travelMinutes + TRAFFIC_BUFFER_MINUTES).coerceAtLeast(1)
    }
}
