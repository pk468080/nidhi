package com.example.nidhi.utils

/**
 * Holds the most-recent location update per booking while the device is offline,
 * so it can be flushed to the Realtime Database once connectivity is restored.
 *
 * Intermediate positions are discarded – only the latest fix per booking matters
 * when catching up after a connectivity gap.
 */
class OfflineTrackingQueue {

    // Keyed by bookingId so we always retain only the freshest position.
    private val pending = mutableMapOf<String, TrackingUpdate>()

    /** Enqueue (or replace) the latest location update for a booking. */
    fun enqueue(update: TrackingUpdate) {
        pending[update.bookingId] = update
    }

    /** Returns true when there are no queued updates. */
    fun isEmpty(): Boolean = pending.isEmpty()

    /**
     * Removes and returns all pending updates, clearing the queue.
     * The caller is responsible for writing them to the database.
     */
    fun drainAll(): List<TrackingUpdate> {
        val snapshot = pending.values.toList()
        pending.clear()
        return snapshot
    }
}

/** Lightweight value object representing a single location snapshot. */
data class TrackingUpdate(
    val bookingId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
