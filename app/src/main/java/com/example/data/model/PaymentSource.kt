package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_sources")
data class PaymentSource(
    @PrimaryKey val name: String,
    val color: String, // Hex code (e.g. "#4DABF7")
    val smartKeywords: String = "", // Comma-separated SMS detection keywords
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    // Sync fields
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: Int = SyncStatus.PENDING
)

