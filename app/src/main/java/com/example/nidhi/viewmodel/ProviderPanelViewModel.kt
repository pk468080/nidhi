package com.example.nidhi.viewmodel

import androidx.lifecycle.ViewModel
import com.example.nidhi.data.model.Booking
import com.example.nidhi.data.model.BookingStatus
import com.example.nidhi.data.repository.BookingRepository
import com.example.nidhi.utils.LocationUtility
import com.example.nidhi.utils.OfflineTrackingQueue
import com.example.nidhi.utils.TrackingStateManager
import com.example.nidhi.utils.TrackingUpdate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProviderPanelUiState(
    val pendingBookings: List<Booking> = emptyList(),
    val hasPendingMore: Boolean = false,
    val isPendingLoadingMore: Boolean = false,
    val activeBookings: List<Booking> = emptyList(),
    val isLoading: Boolean = true,
    val liveTrackingBookingId: String? = null,
    val message: String? = null
)

class ProviderPanelViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repository = BookingRepository()
    private val stateManager = TrackingStateManager(realtimeDb)
    private val offlineQueue = OfflineTrackingQueue()

    private var pendingBookingsListener: ListenerRegistration? = null
    private var activeBookingsListener: ListenerRegistration? = null

    /** Cursor pointing to the last pending-bookings document already fetched. */
    private var lastPendingDocument: DocumentSnapshot? = null

    /** Whether the device currently has a Firebase Realtime DB connection. */
    private var isOnline = true

    // ── Throttle constants ────────────────────────────────────────────────────
    /** Minimum time between location writes (10 seconds → ≤360 writes/hour). */
    private val THROTTLE_INTERVAL_MS = 10_000L
    /** Minimum movement required before writing a new position (50 m). */
    private val MIN_DISTANCE_KM = 0.05

    // ── Per-session tracking state ────────────────────────────────────────────
    private var lastLocationUpdateTime = 0L
    private var lastProviderLat = 0.0
    private var lastProviderLng = 0.0

    private val _uiState = MutableStateFlow(ProviderPanelUiState())
    val uiState: StateFlow<ProviderPanelUiState> = _uiState.asStateFlow()

    fun startListening() {
        pendingBookingsListener?.remove()
        activeBookingsListener?.remove()

        val providerId = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoading = true, message = null) }

        // Monitor Firebase connectivity so location updates can be queued offline.
        stateManager.startMonitoring { online ->
            isOnline = online
            if (online && !offlineQueue.isEmpty()) {
                offlineQueue.drainAll().forEach { update ->
                    writeLocationToDatabase(update.bookingId, update.latitude, update.longitude, update.timestamp)
                }
            }
        }

        // Track whether each query has delivered its first result so that isLoading
        // is cleared only after both listeners have responded at least once.
        var pendingInitialLoad = false
        var activeInitialLoad = false

        fun checkInitialLoadComplete() {
            if (pendingInitialLoad && activeInitialLoad) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        // Query 1: first page of PENDING bookings (one-time paginated fetch).
        // Providers must pull-to-refresh to see bookings that arrive after initial load.
        lastPendingDocument = null
        repository.getPendingBookingsPage(cursor = null) { result ->
            lastPendingDocument = result.lastVisible
            pendingInitialLoad = true
            _uiState.update {
                it.copy(
                    pendingBookings = result.bookings,
                    hasPendingMore = result.hasMore,
                    isPendingLoadingMore = false
                )
            }
            checkInitialLoadComplete()
        }

        // Query 2: active bookings that belong to THIS provider only.
        activeBookingsListener = firestore.collection("bookings_lite")
            .whereEqualTo("providerId", providerId)
            .whereIn(
                "status",
                listOf(
                    BookingStatus.ACCEPTED.value,
                    BookingStatus.ON_THE_WAY.value,
                    BookingStatus.ARRIVED.value
                )
            )
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update {
                        it.copy(isLoading = false, message = "Failed to load provider bookings")
                    }
                    return@addSnapshotListener
                }

                val active = snapshot?.documents
                    ?.mapNotNull { it.toObject(Booking::class.java) }
                    ?: emptyList()

                activeInitialLoad = true
                _uiState.update { it.copy(activeBookings = active) }
                checkInitialLoadComplete()
            }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * Loads the next page of pending bookings and appends it to the existing list.
     * No-op if there are no more pages or a load is already in progress.
     */
    fun loadMorePendingBookings() {
        val state = _uiState.value
        if (!state.hasPendingMore || state.isPendingLoadingMore) return

        _uiState.update { it.copy(isPendingLoadingMore = true) }
        repository.getPendingBookingsPage(cursor = lastPendingDocument) { result ->
            lastPendingDocument = result.lastVisible
            _uiState.update {
                it.copy(
                    pendingBookings = it.pendingBookings + result.bookings,
                    hasPendingMore = result.hasMore,
                    isPendingLoadingMore = false
                )
            }
        }
    }

    /**
     * Reloads the pending bookings list from the first page.
     * Call this from pull-to-refresh to pick up new pending bookings.
     */
    fun refreshPendingBookings() {
        lastPendingDocument = null
        _uiState.update {
            it.copy(
                pendingBookings = emptyList(),
                hasPendingMore = true,
                isPendingLoadingMore = true
            )
        }
        repository.getPendingBookingsPage(cursor = null) { result ->
            lastPendingDocument = result.lastVisible
            _uiState.update {
                it.copy(
                    pendingBookings = result.bookings,
                    hasPendingMore = result.hasMore,
                    isPendingLoadingMore = false
                )
            }
        }
    }

    fun startLiveTracking(bookingId: String) {
        if (bookingId.isBlank()) return
        _uiState.update {
            it.copy(
                liveTrackingBookingId = bookingId,
                message = "Live location sharing started"
            )
        }
    }

    fun stopLiveTracking(bookingId: String? = null) {
        val shouldStop = bookingId == null || _uiState.value.liveTrackingBookingId == bookingId
        if (!shouldStop) return

        _uiState.update {
            it.copy(
                liveTrackingBookingId = null,
                message = "Live location sharing stopped"
            )
        }
    }

    fun updateLiveProviderLocation(bookingId: String, latitude: Double, longitude: Double) {
        if (_uiState.value.liveTrackingBookingId != bookingId) return

        val now = System.currentTimeMillis()

        // Time throttle: skip if fewer than 10 seconds have elapsed since the last write.
        if (now - lastLocationUpdateTime < THROTTLE_INTERVAL_MS) return

        // Distance throttle: skip if the provider hasn't moved at least 50 m.
        if (lastLocationUpdateTime != 0L) {
            val movedKm = LocationUtility.haversineDistance(
                lastProviderLat, lastProviderLng, latitude, longitude
            )
            if (movedKm < MIN_DISTANCE_KM) return
        }

        lastLocationUpdateTime = now
        lastProviderLat = latitude
        lastProviderLng = longitude

        val update = TrackingUpdate(bookingId = bookingId, latitude = latitude, longitude = longitude, timestamp = now)
        if (isOnline) {
            writeLocationToDatabase(bookingId, latitude, longitude, now)
        } else {
            offlineQueue.enqueue(update)
        }
    }

    /**
     * Writes a minimal location payload (lat, lng, updatedAt) to the Realtime Database.
     * Static provider fields (name, phone, rating) live in Firestore and are not duplicated here.
     */
    private fun writeLocationToDatabase(bookingId: String, latitude: Double, longitude: Double, timestamp: Long) {
        realtimeDb.getReference("tracking/$bookingId")
            .updateChildren(
                mapOf(
                    "providerLat" to latitude,
                    "providerLng" to longitude,
                    "updatedAt" to timestamp
                )
            )
    }

    fun acceptBooking(booking: Booking) {
        val providerId = auth.currentUser?.uid ?: "temp_provider"
        val providerName = auth.currentUser?.displayName?.ifBlank { null }
            ?: auth.currentUser?.email?.substringBefore("@")
            ?: "Temporary Provider"

        val providerPhone = auth.currentUser?.phoneNumber.orEmpty()

        firestore.collection("bookings").document(booking.bookingId)
            .update(
                mapOf(
                    "status" to BookingStatus.ACCEPTED.value,
                    "providerId" to providerId,
                    "providerName" to providerName,
                    "providerPhone" to providerPhone,
                    "providerRating" to 4.8
                )
            )
            .addOnSuccessListener {
                // Provider lat/lng start as NaN; real GPS coordinates are written once
                // the provider starts live tracking or bumps their location manually.
                writeTracking(
                    bookingId = booking.bookingId,
                    userId = booking.userId,
                    providerId = providerId,
                    status = BookingStatus.ACCEPTED.value,
                    eta = 0,
                    lat = Double.NaN,
                    lng = Double.NaN
                )
                _uiState.update { it.copy(message = "Booking accepted") }
            }
            .addOnFailureListener {
                _uiState.update { it.copy(message = "Failed to accept booking") }
            }
    }

    fun rejectBooking(bookingId: String) {
        firestore.collection("bookings").document(bookingId)
            .update("status", BookingStatus.REJECTED.value)
            .addOnSuccessListener {
                _uiState.update { it.copy(message = "Booking rejected") }
            }
            .addOnFailureListener {
                _uiState.update { it.copy(message = "Failed to reject booking") }
            }
    }

    fun markOnTheWay(booking: Booking) {
        updateStatusAndTracking(
            booking = booking,
            status = BookingStatus.ON_THE_WAY.value,
            eta = 12
        )
    }

    fun markArrived(booking: Booking) {
        stopLiveTracking(booking.bookingId)
        updateStatusAndTracking(
            booking = booking,
            status = BookingStatus.ARRIVED.value,
            eta = 0
        )
    }

    fun markCompleted(bookingId: String) {
        stopLiveTracking(bookingId)
        firestore.collection("bookings").document(bookingId)
            .update("status", BookingStatus.COMPLETED.value)
            .addOnSuccessListener {
                realtimeDb.getReference("tracking/$bookingId")
                    .updateChildren(mapOf("status" to BookingStatus.COMPLETED.value, "eta" to 0))
                _uiState.update { it.copy(message = "Service marked completed") }
            }
            .addOnFailureListener {
                _uiState.update { it.copy(message = "Failed to complete booking") }
            }
    }

    // Temporary panel helper to emulate provider movement until dedicated provider app is ready.
    fun bumpProviderLocation(booking: Booking) {
        val ref = realtimeDb.getReference("tracking/${booking.bookingId}")
        ref.get().addOnSuccessListener { snap ->
            val currentLat = snap.child("providerLat").getValue(Double::class.java)
            val currentLng = snap.child("providerLng").getValue(Double::class.java)

            // Only bump if a valid GPS location has already been set (e.g. via live tracking).
            if (currentLat == null || currentLng == null ||
                !currentLat.isFinite() || !currentLng.isFinite()
            ) {
                _uiState.update { it.copy(message = "No GPS location set yet. Use live tracking first.") }
                return@addOnSuccessListener
            }

            ref.updateChildren(
                mapOf(
                    "providerLat" to (currentLat - 0.0025),
                    "providerLng" to (currentLng - 0.0020)
                )
            )
            _uiState.update { it.copy(message = "Provider location updated") }
        }
    }

    private fun updateStatusAndTracking(booking: Booking, status: String, eta: Int) {
        firestore.collection("bookings").document(booking.bookingId)
            .update("status", status)
            .addOnSuccessListener {
                realtimeDb.getReference("tracking/${booking.bookingId}")
                    .updateChildren(
                        mapOf(
                            "status" to status,
                            "eta" to eta
                        )
                    )
                _uiState.update { it.copy(message = "Status updated") }
            }
            .addOnFailureListener {
                _uiState.update { it.copy(message = "Failed to update status") }
            }
    }

    /**
     * Creates the initial tracking document when a provider accepts a booking.
     * Only writes the identity fields needed by security rules plus the initial status/ETA.
     * Static provider details (name, phone, rating) are stored in Firestore and are not
     * duplicated in the Realtime Database.
     */
    private fun writeTracking(
        bookingId: String,
        userId: String,
        providerId: String,
        status: String,
        eta: Int,
        lat: Double,
        lng: Double
    ) {
        val now = System.currentTimeMillis()
        realtimeDb.getReference("tracking/$bookingId").setValue(
            mapOf(
                "userId" to userId,
                "providerId" to providerId,
                "status" to status,
                "eta" to eta,
                "providerLat" to lat,
                "providerLng" to lng,
                "updatedAt" to now
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        stateManager.stopMonitoring()
        pendingBookingsListener?.remove()
        activeBookingsListener?.remove()
        _uiState.update { it.copy(liveTrackingBookingId = null) }
    }
}

