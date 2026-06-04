package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val name: String,
    val color: String, // Hex code (e.g. "#FFE600")
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    // Sync fields
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: Int = SyncStatus.PENDING
)
