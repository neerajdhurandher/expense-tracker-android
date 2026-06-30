package com.example.data.export

import com.example.ui.home.YearMonthItem
import java.text.SimpleDateFormat
import java.util.*

sealed class ExportDateRange {
    data object CurrentMonth : ExportDateRange()
    data object Last7Days : ExportDateRange()
    data object Last30Days : ExportDateRange()
    data object Last90Days : ExportDateRange()
    data class Custom(val fromMs: Long, val toMs: Long) : ExportDateRange()

    fun toTimestampRange(): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        return when (this) {
            is CurrentMonth -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(cal.timeInMillis, now)
            }
            is Last7Days  -> Pair(now - 7L  * 86_400_000, now)
            is Last30Days -> Pair(now - 30L * 86_400_000, now)
            is Last90Days -> Pair(now - 90L * 86_400_000, now)
            is Custom     -> Pair(fromMs, toMs)
        }
    }

    fun toPeriodLabel(selectedMonth: YearMonthItem? = null): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val now = System.currentTimeMillis()
        return when (this) {
            is CurrentMonth ->
                selectedMonth?.displayLabel
                    ?: SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
            is Last7Days  ->
                "Last 7 Days (${sdf.format(Date(now - 7L  * 86_400_000))} – ${sdf.format(Date(now))})"
            is Last30Days ->
                "Last 30 Days (${sdf.format(Date(now - 30L * 86_400_000))} – ${sdf.format(Date(now))})"
            is Last90Days ->
                "Last 90 Days (${sdf.format(Date(now - 90L * 86_400_000))} – ${sdf.format(Date(now))})"
            is Custom ->
                "Custom (${sdf.format(Date(fromMs))} – ${sdf.format(Date(toMs))})"
        }
    }
}

data class ExportConfig(
    val dateRange: ExportDateRange = ExportDateRange.CurrentMonth,
    val privacyMode: Boolean = false
)

data class ExportResult(
    val csvContent: String,
    val fileName: String,
    val rowCount: Int
)

