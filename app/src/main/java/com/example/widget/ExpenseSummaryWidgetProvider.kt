package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.ExpenseApp
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

class ExpenseSummaryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidgetAsync(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        when (intent.action) {
            ACTION_SET_INTERVAL -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val interval = intent.getStringExtra(EXTRA_INTERVAL) ?: Interval.MONTH.storageValue
                    saveInterval(context, appWidgetId, interval)
                    updateWidgetAsync(context, appWidgetManager, appWidgetId)
                }
            }

            ACTION_REFRESH -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    updateWidgetAsync(context, appWidgetManager, appWidgetId)
                } else {
                    val ids = appWidgetManager.getAppWidgetIds(
                        ComponentName(context, ExpenseSummaryWidgetProvider::class.java)
                    )
                    ids.forEach { updateWidgetAsync(context, appWidgetManager, it) }
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        appWidgetIds.forEach { editor.remove(intervalKey(it)) }
        editor.apply()
        super.onDeleted(context, appWidgetIds)
    }

    private fun updateWidgetAsync(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        runBlocking(Dispatchers.IO) {
            val app = context.applicationContext as ExpenseApp
            val interval = Interval.fromStorageValue(
                readInterval(context, appWidgetId) ?: Interval.MONTH.storageValue
            )
            val period = periodBounds(interval)

            val current = app.expenseRepository.getTrackedExpensesInDateRange(
                period.currentStartMillis,
                period.currentEndMillis
            )
            val previous = app.expenseRepository.getTrackedExpensesInDateRange(
                period.previousStartMillis,
                period.previousEndMillis
            )

            val currentTotal = current.sumOf { it.amount }
            val previousTotal = previous.sumOf { it.amount }
            val currentCount = current.size

            val views = RemoteViews(context.packageName, R.layout.widget_expense_summary).apply {
                setTextViewText(R.id.widget_interval_chip, interval.displayLabel(context))
                setTextViewText(R.id.widget_amount, formatCurrency(currentTotal))
                setTextViewText(
                    R.id.widget_meta,
                    context.getString(
                        R.string.widget_transactions_format,
                        currentCount,
                        formatTrend(context, currentTotal, previousTotal)
                    )
                )

                setTabStyle(interval)
                bindActions(context, appWidgetId)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun RemoteViews.bindActions(context: Context, appWidgetId: Int) {
        setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 1,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        setOnClickPendingIntent(
            R.id.widget_refresh,
            actionPendingIntent(context, appWidgetId, ACTION_REFRESH, null, 2)
        )
        setOnClickPendingIntent(
            R.id.widget_tab_today,
            actionPendingIntent(context, appWidgetId, ACTION_SET_INTERVAL, Interval.TODAY.storageValue, 3)
        )
        setOnClickPendingIntent(
            R.id.widget_tab_week,
            actionPendingIntent(context, appWidgetId, ACTION_SET_INTERVAL, Interval.WEEK.storageValue, 4)
        )
        setOnClickPendingIntent(
            R.id.widget_tab_month,
            actionPendingIntent(context, appWidgetId, ACTION_SET_INTERVAL, Interval.MONTH.storageValue, 5)
        )
    }

    private fun actionPendingIntent(
        context: Context,
        appWidgetId: Int,
        action: String,
        intervalValue: String?,
        requestCodeSeed: Int
    ): PendingIntent {
        val intent = Intent(context, ExpenseSummaryWidgetProvider::class.java).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            if (intervalValue != null) {
                putExtra(EXTRA_INTERVAL, intervalValue)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + requestCodeSeed,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun RemoteViews.setTabStyle(selected: Interval) {
        val selectedRes = R.drawable.widget_pill_selected_bg
        val unselectedRes = R.drawable.widget_pill_unselected_bg
        setInt(
            R.id.widget_tab_today,
            "setBackgroundResource",
            if (selected == Interval.TODAY) selectedRes else unselectedRes
        )
        setInt(
            R.id.widget_tab_week,
            "setBackgroundResource",
            if (selected == Interval.WEEK) selectedRes else unselectedRes
        )
        setInt(
            R.id.widget_tab_month,
            "setBackgroundResource",
            if (selected == Interval.MONTH) selectedRes else unselectedRes
        )
    }

    private fun formatCurrency(total: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = if (total % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }
        return "₹${formatter.format(total)}"
    }

    private fun formatTrend(context: Context, currentTotal: Double, previousTotal: Double): String {
        if (previousTotal == 0.0) {
            return if (currentTotal == 0.0) {
                context.getString(R.string.widget_no_change)
            } else {
                context.getString(R.string.widget_new_vs_previous)
            }
        }
        val percentage = ((currentTotal - previousTotal) / previousTotal) * 100.0
        val sign = if (percentage >= 0) "+" else "-"
        return context.getString(R.string.widget_trend_format, sign, abs(percentage))
    }

    private fun saveInterval(context: Context, appWidgetId: Int, interval: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(intervalKey(appWidgetId), interval)
            .apply()
    }

    private fun readInterval(context: Context, appWidgetId: Int): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(intervalKey(appWidgetId), null)

    private fun periodBounds(interval: Interval): PeriodBounds {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val nowMillis = now.toInstant().toEpochMilli()

        return when (interval) {
            Interval.TODAY -> {
                val start = startOfDay(now.toLocalDate(), zone)
                val previousStart = start.minusDays(1)
                PeriodBounds(
                    currentStartMillis = start.toInstant().toEpochMilli(),
                    currentEndMillis = nowMillis,
                    previousStartMillis = previousStart.toInstant().toEpochMilli(),
                    previousEndMillis = start.toInstant().minusMillis(1).toEpochMilli()
                )
            }

            Interval.WEEK -> {
                val weekStartDate = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val start = startOfDay(weekStartDate, zone)
                val previousStart = start.minusWeeks(1)
                PeriodBounds(
                    currentStartMillis = start.toInstant().toEpochMilli(),
                    currentEndMillis = nowMillis,
                    previousStartMillis = previousStart.toInstant().toEpochMilli(),
                    previousEndMillis = start.toInstant().minusMillis(1).toEpochMilli()
                )
            }

            Interval.MONTH -> {
                val monthStartDate = now.toLocalDate().withDayOfMonth(1)
                val start = startOfDay(monthStartDate, zone)
                val previousStart = start.minusMonths(1)
                PeriodBounds(
                    currentStartMillis = start.toInstant().toEpochMilli(),
                    currentEndMillis = nowMillis,
                    previousStartMillis = previousStart.toInstant().toEpochMilli(),
                    previousEndMillis = start.toInstant().minusMillis(1).toEpochMilli()
                )
            }
        }
    }

    private fun startOfDay(date: LocalDate, zone: ZoneId): ZonedDateTime = date.atStartOfDay(zone)

    private data class PeriodBounds(
        val currentStartMillis: Long,
        val currentEndMillis: Long,
        val previousStartMillis: Long,
        val previousEndMillis: Long
    )

    private enum class Interval(val storageValue: String) {
        TODAY("today"),
        WEEK("week"),
        MONTH("month");

        fun displayLabel(context: Context): String = when (this) {
            TODAY -> context.getString(R.string.widget_interval_today)
            WEEK -> context.getString(R.string.widget_interval_week)
            MONTH -> context.getString(R.string.widget_interval_month)
        }

        companion object {
            fun fromStorageValue(value: String): Interval =
                entries.firstOrNull { it.storageValue == value } ?: MONTH
        }
    }

    companion object {
        private const val PREFS_NAME = "expense_widget_prefs"
        private const val EXTRA_INTERVAL = "extra_interval"
        private const val ACTION_SET_INTERVAL = "com.example.widget.ACTION_SET_INTERVAL"
        private const val ACTION_REFRESH = "com.example.widget.ACTION_REFRESH"

        private fun intervalKey(appWidgetId: Int): String = "interval_$appWidgetId"
    }
}
