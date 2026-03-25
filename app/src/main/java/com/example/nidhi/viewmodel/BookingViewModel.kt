package com.example.nidhi.viewmodel

import androidx.lifecycle.ViewModel
import com.example.nidhi.data.model.Booking
import com.example.nidhi.data.model.BookingStatus
import com.example.nidhi.data.model.PaymentStatus
import com.example.nidhi.data.repository.BookingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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

    /** True when there are more pages to load. */
    private val _hasMorePages = MutableStateFlow(false)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages.asStateFlow()

    /** Cursor pointing to the last document fetched; null means start from the beginning. */
    private var lastDocument: DocumentSnapshot? = null

    /**
     * Load (or reload) the first page of the user's bookings.
     * Resets pagination state.
     */
    fun loadUserBookings() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true
        lastDocument = null
        repository.getUserBookingsPaged(userId, afterDocument = null) { page, cursor ->
            _bookings.value = page
            lastDocument = cursor
            _hasMorePages.value = page.size >= BookingRepository.PAGE_SIZE
            _isLoading.value = false
        }
    }

    /**
     * Append the next page of bookings to the existing list.
     * No-op if there are no more pages or a load is already in progress.
     */
    fun loadNextPage() {
        if (!_hasMorePages.value || _isLoading.value) return
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true
        repository.getUserBookingsPaged(userId, afterDocument = lastDocument) { page, cursor ->
            _bookings.value = _bookings.value + page
            lastDocument = cursor
            _hasMorePages.value = page.size >= BookingRepository.PAGE_SIZE
            _isLoading.value = false
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
