package com.example.data.model

/** UI model: budget status for a single payment source in a month */
data class SourceBudgetStatus(
    val sourceName: String,
    val sourceColor: String,        // Hex color from PaymentSource
    val budgetAmount: Double,       // Allocated budget (including carry-over)
    val spentAmount: Double,        // Total tracked spending
    val remainingAmount: Double,    // budgetAmount - spentAmount (can be negative)
    val carryOver: Boolean,
    val carryOverAmount: Double = 0.0 // Carried over from previous month
)

/** UI model: aggregated budget summary for a month */
data class BudgetSummary(
    val totalBudget: Double,
    val totalSpent: Double,
    val totalRemaining: Double,
    val utilizationPercent: Float,  // 0.0 to 1.0+ (can exceed 1.0)
    val sourceBudgets: List<SourceBudgetStatus>
)

