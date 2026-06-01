package com.example.data.model

data class ParsedSms(
    val amount: Double,
    val merchant: String?,
    val sender: String,
    val rawSms: String,
    val occurredAt: Long = System.currentTimeMillis(),
    val paymentSource: String = "UPI"
)
