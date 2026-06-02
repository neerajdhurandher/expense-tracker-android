package com.example.data.model

/** Query result holder for spending aggregation by source */
data class SourceSpending(
    val paymentSource: String,
    val total: Double
)

