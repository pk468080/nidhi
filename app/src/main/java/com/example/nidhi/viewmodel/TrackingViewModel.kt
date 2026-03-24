package com.example.nidhi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.nidhi.data.model.BookingStatus
import com.example.nidhi.utils.ETACalculator
import com.example.nidhi.utils.LocationUtility
import com.example.nidhi.utils.TrackingStateManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "TrackingViewModel"

const val DEFAULT_PROVIDER_NAME = "Provider"
const val DEFAULT_PROVIDER_PHONE = ""
const val DEFAULT_PROVIDER_RATING = 0.0
const val DEFAULT_PROVIDER_LAT = Double.NaN
const val DEFAULT_PROVIDER_LNG = Double.NaN
const val INITIAL_ETA_MINUTES = 0


data class TrackingData(
    val bookingId: String = "",
    val serviceName: String = "",
    val status: String = BookingStatus.PENDING.value,
    val eta: Int = INITIAL_ETA_MINUTES,
    val distanceKm: Double = Double.NaN,
    val providerLat: Double = DEFAULT_PROVIDER_LAT,
    val providerLng: Double = DEFAULT_PROVIDER_LNG,
    val customerLat: Double = Double.NaN,
    val customerLng: Double = Double.NaN,
    val providerName: String = DEFAULT_PROVIDER_NAME,
    val providerPhone: String = DEFAULT_PROVIDER_PHONE,
    val providerRating: Double = DEFAULT_PROVIDER_RATING,
    val isOffline: Boolean = false
)

class TrackingViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val stateManager = TrackingStateManager(database)

    private val _trackingData = MutableStateFlow(TrackingData())
    val trackingData: StateFlow<TrackingData> = _trackingData.asStateFlow()

    private var trackingListener: ValueEventListener? = null
    private var trackingRef: DatabaseReference? = null
    private var bookingListener: ListenerRegistration? = null

    fun startTracking(bookingId: String) {
        val userId = auth.currentUser?.uid ?: return
        if (bookingId.isBlank()) return

        bookingListener?.remove()
        trackingListener?.let { trackingRef?.removeEventListener(it) }

        _trackingData.value = TrackingData(bookingId = bookingId)

        stateManager.startMonitoring { isOnline ->
            _trackingData.update { it.copy(isOffline = !isOnline) }
        }

        bookingListener = firestore.collection("bookings").document(bookingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Booking listener error: ${error.message}", error)
                    return@addSnapshotListener
                }

                val booking = snapshot?.data ?: return@addSnapshotListener
                if (booking["userId"] as? String != userId) return@addSnapshotListener

                _trackingData.update { current ->
                    current.copy(
                        serviceName = booking["serviceName"] as? String ?: current.serviceName,
                        status = booking["status"] as? String ?: current.status,
                        providerName = booking["providerName"] as? String ?: current.providerName,
                        providerPhone = booking["providerPhone"] as? String ?: current.providerPhone,
                        providerRating = booking["providerRating"] as? Double ?: current.providerRating
                    )
                }
            }

        val ref = database.getReference("tracking/$bookingId")
        trackingRef = ref

        // Attach real-time listener
        trackingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _trackingData.update { current ->
                    val rawLat = snapshot.child("providerLat").getValue(Double::class.java)
                    val rawLng = snapshot.child("providerLng").getValue(Double::class.java)

                    // Validate incoming provider coordinates before accepting them
                    val (newLat, newLng) = if (
                        rawLat != null && rawLng != null &&
                        LocationUtility.isValidCoordinate(rawLat, rawLng)
                    ) {
                        LocationUtility.smoothLocation(
                            current.providerLat, current.providerLng, rawLat, rawLng
                        )
                    } else {
                        Pair(current.providerLat, current.providerLng)
                    }

                    val updatedStatus = snapshot.child("status").getValue(String::class.java)
                        ?: current.status

                    val updated = current.copy(
                        status = updatedStatus,
                        providerLat = newLat,
                        providerLng = newLng,
                        providerName = snapshot.child("providerName").getValue(String::class.java)
                            ?: current.providerName,
                        providerPhone = snapshot.child("providerPhone").getValue(String::class.java)
                            ?: current.providerPhone,
                        providerRating = snapshot.child("providerRating").getValue(Double::class.java)
                            ?: current.providerRating
                    )
                    recalculateEtaAndDistance(updated)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Tracking listener cancelled: ${error.message}", error.toException())
            }
        }
        ref.addValueEventListener(trackingListener!!)
    }

    /**
     * Called by the UI layer whenever the customer's GPS location changes.
     * Triggers an ETA and distance recalculation.
     */
    fun updateCustomerLocation(lat: Double, lng: Double) {
        if (!LocationUtility.isValidCoordinate(lat, lng)) return
        _trackingData.update { current ->
            recalculateEtaAndDistance(current.copy(customerLat = lat, customerLng = lng))
        }
    }

    /** Derives [TrackingData.distanceKm] and [TrackingData.eta] from the current coordinates. */
    private fun recalculateEtaAndDistance(data: TrackingData): TrackingData {
        val hasProvider = LocationUtility.isValidCoordinate(data.providerLat, data.providerLng)
        val hasCustomer = LocationUtility.isValidCoordinate(data.customerLat, data.customerLng)

        if (!hasProvider || !hasCustomer) return data

        val distanceKm = LocationUtility.haversineDistance(
            data.providerLat, data.providerLng,
            data.customerLat, data.customerLng
        )
        val eta = ETACalculator.calculateEta(distanceKm)
        return data.copy(distanceKm = distanceKm, eta = eta)
    }

    override fun onCleared() {
        super.onCleared()
        stateManager.stopMonitoring()
        bookingListener?.remove()
        trackingListener?.let { trackingRef?.removeEventListener(it) }
    }
}

