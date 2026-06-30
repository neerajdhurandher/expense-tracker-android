package com.example.data.model

import androidx.room.Entity

@Entity(
    tableName = "source_budgets",
    primaryKeys = ["sourceName", "yearMonth"]
)
data class SourceBudget(
    val sourceName: String,         // Maps to PaymentSource.name (e.g. "UPI")
    val yearMonth: String,          // Format: "yyyy-MM" (e.g. "2026-06")
    val amount: Double,             // Budget amount for this source in this month
    val carryOver: Boolean = false, // Whether to carry unspent to next month
    val createdAt: Long = System.currentTimeMillis(),
    // Sync fields
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: Int = SyncStatus.PENDING
)

