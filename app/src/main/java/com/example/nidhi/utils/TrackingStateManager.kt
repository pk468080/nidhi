package com.example.nidhi.utils

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

private const val TAG = "TrackingStateManager"

/**
 * Monitors Firebase Realtime Database connectivity and exposes the online/offline
 * state via [onConnectivityChanged].
 *
 * Firebase exposes a special `.info/connected` boolean that reflects whether the
 * SDK currently has an active connection to the backend, making it ideal for
 * detecting offline mode without requiring Android Context.
 */
class TrackingStateManager(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    private var connectivityListener: ValueEventListener? = null

    /**
     * Start listening for connectivity changes.
     * [onConnectivityChanged] receives `true` when online, `false` when offline.
     */
    fun startMonitoring(onConnectivityChanged: (isOnline: Boolean) -> Unit) {
        stopMonitoring()
        val ref = database.getReference(".info/connected")
        connectivityListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.getValue(Boolean::class.java) ?: false
                onConnectivityChanged(isOnline)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Connectivity listener cancelled: ${error.message}", error.toException())
            }
        }
        ref.addValueEventListener(connectivityListener!!)
    }

    /** Remove the Firebase connectivity listener. */
    fun stopMonitoring() {
        connectivityListener?.let {
            database.getReference(".info/connected").removeEventListener(it)
        }
        connectivityListener = null
    }
}
