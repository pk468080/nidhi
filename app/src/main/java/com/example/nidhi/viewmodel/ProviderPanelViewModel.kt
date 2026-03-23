package com.example.nidhi.viewmodel

import androidx.lifecycle.ViewModel
import com.example.nidhi.data.model.Booking
import com.example.nidhi.data.model.BookingStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProviderPanelUiState(
    val pendingBookings: List<Booking> = emptyList(),
    val activeBookings: List<Booking> = emptyList(),
    val isLoading: Boolean = true,
    val liveTrackingBookingId: String? = null,
    val message: String? = null
)

class ProviderPanelViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var bookingsListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(ProviderPanelUiState())
    val uiState: StateFlow<ProviderPanelUiState> = _uiState.asStateFlow()

    fun startListening() {
        bookingsListener?.remove()
        _uiState.update { it.copy(isLoading = true, message = null) }

        bookingsListener = firestore.collection("bookings")
            .whereIn(
                "status",
                listOf(
                    BookingStatus.PENDING.value,
                    BookingStatus.ACCEPTED.value,
                    BookingStatus.ON_THE_WAY.value,
                    BookingStatus.ARRIVED.value
                )
            )
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update {
                        it.copy(isLoading = false, message = "Failed to load provider bookings")
                    }
                    return@addSnapshotListener
                }

                val bookings = snapshot?.documents
                    ?.mapNotNull { it.toObject(Booking::class.java) }
                    ?.sortedByDescending { it.timestamp }
                    ?: emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingBookings = bookings.filter { booking ->
                            booking.status == BookingStatus.PENDING.value
                        },
                        activeBookings = bookings.filter { booking ->
                            booking.status in listOf(
                                BookingStatus.ACCEPTED.value,
                                BookingStatus.ON_THE_WAY.value,
                                BookingStatus.ARRIVED.value
                            )
                        }
                    )
                }
            }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
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

        realtimeDb.getReference("tracking/$bookingId")
            .updateChildren(
                mapOf(
                    "providerLat" to latitude,
                    "providerLng" to longitude,
                    "status" to BookingStatus.ON_THE_WAY.value
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
                writeTracking(
                    bookingId = booking.bookingId,
                    status = BookingStatus.ACCEPTED.value,
                    eta = 20,
                    lat = 28.6250,
                    lng = 77.2200,
                    providerName = providerName,
                    providerPhone = providerPhone,
                    providerRating = 4.8
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
            val currentLat = snap.child("providerLat").getValue(Double::class.java) ?: 28.6250
            val currentLng = snap.child("providerLng").getValue(Double::class.java) ?: 77.2200
            val currentEta = snap.child("eta").getValue(Long::class.java)?.toInt() ?: 10

            ref.updateChildren(
                mapOf(
                    "providerLat" to (currentLat - 0.0025),
                    "providerLng" to (currentLng - 0.0020),
                    "eta" to (currentEta - 2).coerceAtLeast(1)
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

    private fun writeTracking(
        bookingId: String,
        status: String,
        eta: Int,
        lat: Double,
        lng: Double,
        providerName: String,
        providerPhone: String,
        providerRating: Double
    ) {
        realtimeDb.getReference("tracking/$bookingId").setValue(
            mapOf(
                "status" to status,
                "eta" to eta,
                "providerLat" to lat,
                "providerLng" to lng,
                "providerName" to providerName,
                "providerPhone" to providerPhone,
                "providerRating" to providerRating
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        bookingsListener?.remove()
        _uiState.update { it.copy(liveTrackingBookingId = null) }
    }
}

