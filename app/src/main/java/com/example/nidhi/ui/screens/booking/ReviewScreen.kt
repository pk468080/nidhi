package com.example.nidhi.ui.screens.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nidhi.data.model.Booking
import com.example.nidhi.data.model.Review
import com.example.nidhi.data.repository.ReviewRepository
import com.example.nidhi.ui.theme.AppSpacing
import com.example.nidhi.ui.theme.Warning
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    bookingId: String,
    navController: NavController
) {
    val firestore = FirebaseFirestore.getInstance()
    val reviewRepository = remember { ReviewRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var booking by remember { mutableStateOf<Booking?>(null) }
    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var alreadyReviewed by remember { mutableStateOf(false) }

    LaunchedEffect(bookingId) {
        if (bookingId.isBlank() || currentUser == null) return@LaunchedEffect

        firestore.collection("bookings").document(bookingId)
            .get()
            .addOnSuccessListener { snapshot ->
                booking = snapshot.toObject(Booking::class.java)
            }

        reviewRepository.hasReviewForBooking(bookingId, currentUser.uid) { exists ->
            alreadyReviewed = exists
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Leave a Review") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AppSpacing.default)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.default)
        ) {
            if (alreadyReviewed) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "You have already submitted a review for this booking.",
                        modifier = Modifier.padding(AppSpacing.default),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                return@Column
            }

            booking?.let { b ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.default)) {
                        Text(
                            text = b.serviceName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (b.providerName.isNotEmpty()) {
                            Text(
                                text = "Provider: ${b.providerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Text(
                text = "Rate your experience",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                for (star in 1..5) {
                    IconButton(onClick = { rating = star }) {
                        Icon(
                            imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "$star stars",
                            tint = if (star <= rating) Warning else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Write a comment (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                shape = MaterialTheme.shapes.medium
            )

            Button(
                onClick = {
                    val uid = currentUser?.uid ?: return@Button
                    val b = booking ?: return@Button
                    if (rating == 0) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please select a star rating")
                        }
                        return@Button
                    }
                    isSubmitting = true
                    val review = Review(
                        bookingId = bookingId,
                        customerId = uid,
                        providerId = b.providerId,
                        serviceName = b.serviceName,
                        rating = rating.toFloat(),
                        comment = comment.trim()
                    )
                    reviewRepository.submitReview(review) { success ->
                        isSubmitting = false
                        if (success) {
                            navController.popBackStack()
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to submit review. Please try again.")
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Submit Review",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.default))
        }
    }
}
