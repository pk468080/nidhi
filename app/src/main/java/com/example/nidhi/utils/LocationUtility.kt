package com.example.nidhi.utils

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility functions for GPS coordinate validation, distance calculation, and location smoothing.
 */
object LocationUtility {

    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Returns true if lat/lng represent a valid GPS coordinate.
     */
    fun isValidCoordinate(lat: Double, lng: Double): Boolean =
        lat.isFinite() && lng.isFinite() && lat in -90.0..90.0 && lng in -180.0..180.0

    /**
     * Haversine formula: great-circle distance between two points in kilometres.
     */
    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return EARTH_RADIUS_KM * 2 * asin(sqrt(a))
    }

    /**
     * Exponential moving average to smooth jittery GPS updates.
     * [factor] controls how much weight is given to the new position (0 < factor <= 1).
     * A value of 1.0 means no smoothing (accept raw position as-is).
     */
    fun smoothLocation(
        currentLat: Double,
        currentLng: Double,
        newLat: Double,
        newLng: Double,
        factor: Double = 0.7
    ): Pair<Double, Double> {
        if (!isValidCoordinate(currentLat, currentLng)) return Pair(newLat, newLng)
        return Pair(
            currentLat * (1 - factor) + newLat * factor,
            currentLng * (1 - factor) + newLng * factor
        )
    }
}
