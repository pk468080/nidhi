package com.example.nidhi.data.repository

import com.example.nidhi.data.model.Payment
import com.example.nidhi.data.model.PaymentStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PaymentRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun savePayment(payment: Payment, onResult: (Boolean, String?) -> Unit) {
        val docRef = firestore.collection("payments").document()
        val paymentWithId = payment.copy(paymentId = docRef.id)
        docRef.set(paymentWithId)
            .addOnSuccessListener { onResult(true, docRef.id) }
            .addOnFailureListener { onResult(false, null) }
    }

    /**
     * One-time fetch of the user's payment history (most recent 50 records).
     * Replaced the previous addSnapshotListener to avoid continuous read charges.
     */
    fun getUserTransactions(userId: String, onResult: (List<Payment>) -> Unit) {
        firestore.collection("payments")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snapshot ->
                val payments = snapshot.documents.mapNotNull { it.toObject(Payment::class.java) }
                onResult(payments)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun updatePaymentStatus(
        paymentId: String,
        status: PaymentStatus,
        transactionId: String,
        onResult: (Boolean) -> Unit
    ) {
        firestore.collection("payments").document(paymentId)
            .update(mapOf("status" to status.value, "transactionId" to transactionId))
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}
