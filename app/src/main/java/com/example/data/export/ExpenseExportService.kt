package com.example.data.export

import com.example.data.model.Expense
import java.text.SimpleDateFormat
import java.util.*

object ExpenseExportService {

    fun generateCsv(
        expenses: List<Expense>,
        config: ExportConfig,
        periodLabel: String
    ): ExportResult {
        val now = System.currentTimeMillis()
        val dateFmt     = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val timeFmt     = SimpleDateFormat("hh:mm a",     Locale.US)
        val fullFmt     = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        val fileSlugFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

        val total = expenses.sumOf { it.amount }
        val sb = StringBuilder()

        // ── Branded header ────────────────────────────────────────────────────
        sb.appendLine("EXPENSE TRACKER REPORT")
        sb.appendLine("Generated On,${fullFmt.format(Date(now))}")
        sb.appendLine("Period,${csv(periodLabel)}")
        sb.appendLine("Total Transactions,${expenses.size}")
        sb.appendLine("Total Amount (INR),${String.format(Locale.US, "%,.2f", total)}")
        sb.appendLine("Privacy Mode,${if (config.privacyMode) "ON – SMS details masked" else "OFF"}")
        sb.appendLine()

        // ── All Expenses ──────────────────────────────────────────────────────
        sb.appendLine("ALL EXPENSES")
        sb.appendLine("#,Date,Time,Merchant,Amount (INR),Category,Payment Source,Mode,Sender,Raw SMS")

        expenses.forEachIndexed { i, e ->
            val sender = if (config.privacyMode) mask(e.sender) else csv(e.sender ?: "-")
            val rawSms = if (config.privacyMode) "***" else csv(e.rawSms ?: "-")
            sb.appendLine(
                listOf(
                    "${i + 1}",
                    dateFmt.format(Date(e.occurredAt)),
                    timeFmt.format(Date(e.occurredAt)),
                    csv(e.name),
                    String.format(Locale.US, "%.2f", e.amount),
                    e.category,
                    e.paymentSource,
                    e.source,
                    sender,
                    rawSms
                ).joinToString(",")
            )
        }

        // ── Category Summary ──────────────────────────────────────────────────
        sb.appendLine()
        sb.appendLine("CATEGORY SUMMARY")
        sb.appendLine("Category,Total Amount (INR),Transactions,% Share")
        expenses.groupBy { it.category }
            .entries.sortedByDescending { (_, v) -> v.sumOf { it.amount } }
            .forEach { (cat, items) ->
                val t   = items.sumOf { it.amount }
                val pct = if (total > 0) t / total * 100 else 0.0
                sb.appendLine(
                    "$cat,${String.format(Locale.US, "%.2f", t)},${items.size},${String.format(Locale.US, "%.1f%%", pct)}"
                )
            }

        // ── Source Summary ────────────────────────────────────────────────────
        sb.appendLine()
        sb.appendLine("SOURCE SUMMARY")
        sb.appendLine("Payment Source,Total Amount (INR),Transactions,% Share")
        expenses.groupBy { it.paymentSource }
            .entries.sortedByDescending { (_, v) -> v.sumOf { it.amount } }
            .forEach { (src, items) ->
                val t   = items.sumOf { it.amount }
                val pct = if (total > 0) t / total * 100 else 0.0
                sb.appendLine(
                    "$src,${String.format(Locale.US, "%.2f", t)},${items.size},${String.format(Locale.US, "%.1f%%", pct)}"
                )
            }

        val slug     = periodLabel.replace(Regex("[^A-Za-z0-9]"), "_").take(25)
        val fileName = "ExpenseReport_${slug}_${fileSlugFmt.format(Date(now))}.csv"

        return ExportResult(
            csvContent = sb.toString(),
            fileName   = fileName,
            rowCount   = expenses.size
        )
    }

    /** Wrap in double-quotes if value contains commas, quotes, or newlines. */
    private fun csv(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${value.replace("\"", "\"\"").replace("\n", " ").replace("\r", "")}\""
        } else value

    /** Show first 2 chars + *** for privacy masking. */
    private fun mask(value: String?): String {
        if (value.isNullOrBlank()) return "-"
        return if (value.length <= 3) "***" else "${value.take(2)}***"
    }
}

