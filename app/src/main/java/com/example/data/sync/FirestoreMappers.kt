package com.example.data.sync

import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.PaymentSource
import com.example.data.model.SourceBudget
import com.example.data.model.SyncStatus
import com.google.firebase.firestore.DocumentSnapshot
import java.util.UUID

// ═══════════════════════════════════════════════════════════════
// Expense ↔ Firestore
// ═══════════════════════════════════════════════════════════════

fun Expense.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "amount" to amount,
    "category" to category,
    "currency" to currency,
    "source" to source,
    "rawSms" to rawSms,
    "sender" to sender,
    "occurredAt" to occurredAt,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "yearMonth" to yearMonth,
    "paymentSource" to paymentSource,
    "isTracked" to isTracked,
    "isDeleted" to isDeleted
)

fun DocumentSnapshot.toExpense(): Expense? {
    return try {
        Expense(
            id = 0, // Room will assign local ID
            firestoreId = id,
            name = getString("name") ?: return null,
            amount = getDouble("amount") ?: return null,
            category = getString("category") ?: "Other",
            currency = getString("currency") ?: "INR",
            source = getString("source") ?: "manual",
            rawSms = getString("rawSms"),
            sender = getString("sender"),
            occurredAt = getLong("occurredAt") ?: System.currentTimeMillis(),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
            yearMonth = getString("yearMonth") ?: "",
            paymentSource = getString("paymentSource") ?: "UPI",
            isTracked = getBoolean("isTracked") ?: true,
            isDeleted = getBoolean("isDeleted") ?: false,
            syncStatus = SyncStatus.SYNCED
        )
    } catch (e: Exception) {
        null
    }
}

// ═══════════════════════════════════════════════════════════════
// Category ↔ Firestore
// ═══════════════════════════════════════════════════════════════

fun Category.toFirestoreMap(): Map<String, Any?> = mapOf(
    "color" to color,
    "isCustom" to isCustom,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "isDeleted" to isDeleted
)

fun DocumentSnapshot.toCategory(): Category? {
    return try {
        Category(
            name = id, // Document ID = category name
            color = getString("color") ?: "#ADB5BD",
            isCustom = getBoolean("isCustom") ?: false,
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
            isDeleted = getBoolean("isDeleted") ?: false,
            syncStatus = SyncStatus.SYNCED
        )
    } catch (e: Exception) {
        null
    }
}

// ═══════════════════════════════════════════════════════════════
// PaymentSource ↔ Firestore
// ═══════════════════════════════════════════════════════════════

fun PaymentSource.toFirestoreMap(): Map<String, Any?> = mapOf(
    "color" to color,
    "smartKeywords" to smartKeywords,
    "isCustom" to isCustom,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "isDeleted" to isDeleted
)

fun DocumentSnapshot.toPaymentSource(): PaymentSource? {
    return try {
        PaymentSource(
            name = id, // Document ID = source name
            color = getString("color") ?: "#4DABF7",
            smartKeywords = getString("smartKeywords") ?: "",
            isCustom = getBoolean("isCustom") ?: false,
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
            isDeleted = getBoolean("isDeleted") ?: false,
            syncStatus = SyncStatus.SYNCED
        )
    } catch (e: Exception) {
        null
    }
}

// ═══════════════════════════════════════════════════════════════
// SourceBudget ↔ Firestore
// ═════════════���═════════════════════════════════════════════════

/** Composite document ID for source budgets: "sourceName_yearMonth" */
fun SourceBudget.firestoreDocId(): String = "${sourceName}_${yearMonth}"

fun SourceBudget.toFirestoreMap(): Map<String, Any?> = mapOf(
    "sourceName" to sourceName,
    "yearMonth" to yearMonth,
    "amount" to amount,
    "carryOver" to carryOver,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "isDeleted" to isDeleted
)

fun DocumentSnapshot.toSourceBudget(): SourceBudget? {
    return try {
        SourceBudget(
            sourceName = getString("sourceName") ?: return null,
            yearMonth = getString("yearMonth") ?: return null,
            amount = getDouble("amount") ?: 0.0,
            carryOver = getBoolean("carryOver") ?: false,
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
            isDeleted = getBoolean("isDeleted") ?: false,
            syncStatus = SyncStatus.SYNCED
        )
    } catch (e: Exception) {
        null
    }
}

