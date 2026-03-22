package com.example.nidhi.data.repository

import com.example.nidhi.data.model.Payment
import com.example.nidhi.data.model.PaymentStatus
import com.google.firebase.firestore.FirebaseFirestore

class PaymentRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun savePayment(payment: Payment, onResult: (Boolean, String?) -> Unit) {
        val docRef = firestore.collection("payments").document()
        val paymentWithId = payment.copy(paymentId = docRef.id)
        docRef.set(paymentWithId)
            .addOnSuccessListener { onResult(true, docRef.id) }
            .addOnFailureListener { onResult(false, null) }
    }

    fun getUserTransactions(userId: String, onResult: (List<Payment>) -> Unit) {
        firestore.collection("payments")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                val payments = snapshot?.documents?.mapNotNull {
                    it.toObject(Payment::class.java)
                } ?: emptyList()
                onResult(payments)
            }
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
