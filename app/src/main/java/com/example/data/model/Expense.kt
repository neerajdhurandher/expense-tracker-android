package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val category: String,
    val currency: String = "INR",
    val source: String, // "sms" or "manual"
    val rawSms: String? = null,
    val sender: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val yearMonth: String, // Format: YYYY-MM
    val paymentSource: String = "UPI", // e.g. "Cash", "UPI", "Credit Card"
    val isTracked: Boolean = true, // false = untracked SMS expense pending user action
    // Sync fields
    val firestoreId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: Int = SyncStatus.PENDING
)
