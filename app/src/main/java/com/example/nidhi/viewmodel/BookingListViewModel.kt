package com.example.nidhi.viewmodel

import androidx.lifecycle.ViewModel
import com.example.nidhi.data.model.PaginationState
import com.example.nidhi.data.repository.BookingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for the customer's booking history list.
 * Uses cursor-based pagination via [BookingRepository.getBookingsPage] to avoid
 * loading the entire booking history on screen entry.
 */
class BookingListViewModel : ViewModel() {

    private val repository = BookingRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _paginationState = MutableStateFlow(PaginationState())
    val paginationState: StateFlow<PaginationState> = _paginationState.asStateFlow()

    init {
        loadNextPage()
    }

    /**
     * Loads the next page of bookings and appends it to the existing list.
     * No-op if there are no more pages or a load is already in progress.
     */
    fun loadNextPage() {
        val state = _paginationState.value
        if (!state.hasMore || state.isLoading) return
        val userId = auth.currentUser?.uid ?: return

        _paginationState.update { it.copy(isLoading = true) }
        repository.getBookingsPage(
            userId = userId,
            cursor = state.lastVisible,
            pageSize = PAGE_SIZE
        ) { result ->
            _paginationState.update {
                it.copy(
                    items = it.items + result.bookings,
                    lastVisible = result.lastVisible,
                    hasMore = result.hasMore,
                    isLoading = false
                )
            }
        }
    }

    /** Resets pagination and reloads from the first page. */
    fun refresh() {
        _paginationState.value = PaginationState()
        loadNextPage()
    }

    companion object {
        const val PAGE_SIZE = BookingRepository.CUSTOMER_PAGE_SIZE
    }
}
