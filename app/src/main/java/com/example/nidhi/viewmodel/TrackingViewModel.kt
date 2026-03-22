package com.example.nidhi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "TrackingViewModel"

// Default provider/location constants shared by TrackingViewModel and BookingScreen
const val DEFAULT_PROVIDER_NAME = "Rahul Sharma"
const val DEFAULT_PROVIDER_PHONE = "+91 98765 43210"
const val DEFAULT_PROVIDER_RATING = 4.8
const val DEFAULT_PROVIDER_LAT = 28.6250
const val DEFAULT_PROVIDER_LNG = 77.2200
const val INITIAL_ETA_MINUTES = 15

// User (destination) location
private const val USER_LAT = 28.6139
private const val USER_LNG = 77.2090

data class TrackingData(
    val status: String = "accepted",
    val eta: Int = INITIAL_ETA_MINUTES,
    val providerLat: Double = DEFAULT_PROVIDER_LAT,
    val providerLng: Double = DEFAULT_PROVIDER_LNG,
    val providerName: String = DEFAULT_PROVIDER_NAME,
    val providerPhone: String = DEFAULT_PROVIDER_PHONE,
    val providerRating: Double = DEFAULT_PROVIDER_RATING
)

class TrackingViewModel : ViewModel() {

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _trackingData = MutableStateFlow(TrackingData())
    val trackingData: StateFlow<TrackingData> = _trackingData.asStateFlow()

    private var trackingListener: ValueEventListener? = null
    private var trackingRef: DatabaseReference? = null
    private var simulationJob: Job? = null

    fun startTracking(serviceName: String) {
        val userId = auth.currentUser?.uid ?: return
        val safeServiceName = serviceName.replace(" ", "_")
        val ref = database.getReference("tracking/$userId/$safeServiceName")
        trackingRef = ref

        // Write initial provider location and status to Realtime Database
        ref.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val initialData = buildInitialTrackingData()
                ref.setValue(initialData)
                    .addOnSuccessListener { startSimulation(ref) }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to initialize tracking data", e)
                        // Fall back to local simulation so the UI still works
                        startSimulation(ref)
                    }
            } else {
                startSimulation(ref)
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to read tracking node", e)
        }

        // Attach real-time listener
        trackingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _trackingData.update { current ->
                    current.copy(
                        status = snapshot.child("status").getValue(String::class.java) ?: current.status,
                        eta = snapshot.child("eta").getValue(Long::class.java)?.toInt() ?: current.eta,
                        providerLat = snapshot.child("providerLat").getValue(Double::class.java) ?: current.providerLat,
                        providerLng = snapshot.child("providerLng").getValue(Double::class.java) ?: current.providerLng,
                        providerName = snapshot.child("providerName").getValue(String::class.java) ?: current.providerName,
                        providerPhone = snapshot.child("providerPhone").getValue(String::class.java) ?: current.providerPhone,
                        providerRating = snapshot.child("providerRating").getValue(Double::class.java) ?: current.providerRating
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Tracking listener cancelled: ${error.message}", error.toException())
            }
        }
        ref.addValueEventListener(trackingListener!!)
    }

    private fun buildInitialTrackingData() = mapOf(
        "status" to "accepted",
        "eta" to INITIAL_ETA_MINUTES,
        "providerLat" to DEFAULT_PROVIDER_LAT,
        "providerLng" to DEFAULT_PROVIDER_LNG,
        "providerName" to DEFAULT_PROVIDER_NAME,
        "providerPhone" to DEFAULT_PROVIDER_PHONE,
        "providerRating" to DEFAULT_PROVIDER_RATING
    )

    private fun startSimulation(ref: DatabaseReference) {
        // Cancel any previously running simulation before starting a new one
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            var currentLat = DEFAULT_PROVIDER_LAT
            var currentLng = DEFAULT_PROVIDER_LNG

            for (step in 1..12) {
                delay(3000L)

                // Gradually move provider towards user location
                currentLat -= (currentLat - USER_LAT) * 0.25
                currentLng -= (currentLng - USER_LNG) * 0.25

                val newEta = (INITIAL_ETA_MINUTES.toDouble() * (1.0 - step / 12.0)).toInt().coerceAtLeast(1)
                val newStatus = when {
                    step <= 3 -> "accepted"
                    step <= 9 -> "on_the_way"
                    else -> "arrived"
                }

                ref.updateChildren(
                    mapOf(
                        "providerLat" to currentLat,
                        "providerLng" to currentLng,
                        "eta" to newEta,
                        "status" to newStatus
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
        trackingListener?.let { trackingRef?.removeEventListener(it) }
    }
}

