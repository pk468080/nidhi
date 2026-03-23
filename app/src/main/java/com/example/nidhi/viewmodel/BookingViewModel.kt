package com.example.nidhi.viewmodel

import androidx.lifecycle.ViewModel
import com.example.nidhi.data.model.Booking
import com.example.nidhi.data.model.BookingStatus
import com.example.nidhi.data.model.PaymentStatus
import com.example.nidhi.data.repository.BookingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
class BookingViewModel : ViewModel() {

    private val repository = BookingRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _bookingResult = MutableStateFlow<BookingResult?>(null)
    val bookingResult: StateFlow<BookingResult?> = _bookingResult.asStateFlow()

    fun loadUserBookings() {
        val userId = auth.currentUser?.uid ?: return
        repository.getUserBookings(userId) { bookings ->
            _bookings.value = bookings
        }
    }

    fun createBooking(
        serviceName: String,
        address: String,
        scheduledDate: String,
        scheduledTime: String,
        amount: Double,
        notes: String
    ) {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        val booking = Booking(
            serviceName = serviceName,
            address = address,
            userId = userId,
            status = BookingStatus.PENDING.value,
            scheduledDate = scheduledDate,
            scheduledTime = scheduledTime,
            amount = amount,
            notes = notes,
            paymentStatus = PaymentStatus.PENDING.value
        )

        repository.createBooking(booking) { success, bookingId ->
            _isLoading.value = false
            if (success && bookingId != null) {
                _bookingResult.value = BookingResult.Success(bookingId, serviceName, amount)
            } else {
                _bookingResult.value = BookingResult.Failure("Failed to create booking. Please try again.")
            }
        }
    }

    fun clearBookingResult() {
        _bookingResult.value = null
    }
}

sealed class BookingResult {
    data class Success(val bookingId: String, val serviceName: String, val amount: Double) : BookingResult()
    data class Failure(val message: String) : BookingResult()
}
