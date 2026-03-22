package com.example.nidhi.data.model

data class Payment(
    val paymentId: String = "",
    val bookingId: String = "",
    val userId: String = "",
    val serviceName: String = "",
    val amount: Double = 0.0,
    val method: String = PaymentMethod.UPI.value,
    val status: String = PaymentStatus.PENDING.value,
    val transactionId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class PaymentMethod(val value: String, val displayName: String) {
    UPI("upi", "UPI"),
    CARD("card", "Credit / Debit Card"),
    WALLET("wallet", "Wallet"),
    NET_BANKING("net_banking", "Net Banking")
}
