package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.annotation.StringRes
import com.example.ExpenseApp
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.abs

enum class FixedInterval {
    TODAY,
    MONTH
}

abstract class FixedIntervalExpenseWidgetProvider(
    private val interval: FixedInterval,
    @param:StringRes private val titleRes: Int,
    private val layoutRes: Int
) : AppWidgetProvider() {

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
        if (intent.action != ACTION_REFRESH) return

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            updateWidgetAsync(context, appWidgetManager, appWidgetId)
            return
        }

        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, javaClass))
        ids.forEach { updateWidgetAsync(context, appWidgetManager, it) }
    }

    private fun updateWidgetAsync(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        runBlocking(Dispatchers.IO) {
            val app = context.applicationContext as ExpenseApp
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

            val views = RemoteViews(context.packageName, layoutRes).apply {
                setTextViewText(R.id.widget_title, context.getString(titleRes))
                setTextViewText(R.id.widget_interval_chip, intervalLabel(context, interval))
                setTextViewText(R.id.widget_amount, formatCurrency(currentTotal))
                setTextViewText(
                    R.id.widget_meta,
                    context.getString(
                        R.string.widget_transactions_format,
                        currentCount,
                        formatTrend(context, currentTotal, previousTotal)
                    )
                )

                // Keep only a single interval visible for dedicated widget variants.
                setViewVisibility(R.id.widget_tab_today, android.view.View.GONE)
                setViewVisibility(R.id.widget_tab_week, android.view.View.GONE)
                setViewVisibility(R.id.widget_tab_month, android.view.View.GONE)
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
            PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 2,
                Intent(context, javaClass).apply {
                    action = ACTION_REFRESH
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    private fun formatCurrency(total: Double): String {
        val formatter = NumberFormat.getNumberInstance(
            Locale.Builder().setLanguage("en").setRegion("IN").build()
        ).apply {
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

    private fun intervalLabel(context: Context, interval: FixedInterval): String = when (interval) {
        FixedInterval.TODAY -> context.getString(R.string.widget_interval_today)
        FixedInterval.MONTH -> context.getString(R.string.widget_interval_month)
    }

    private fun periodBounds(interval: FixedInterval): PeriodBounds {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val nowMillis = now.toInstant().toEpochMilli()

        return when (interval) {
            FixedInterval.TODAY -> {
                val start = startOfDay(now.toLocalDate(), zone)
                val previousStart = start.minusDays(1)
                PeriodBounds(
                    currentStartMillis = start.toInstant().toEpochMilli(),
                    currentEndMillis = nowMillis,
                    previousStartMillis = previousStart.toInstant().toEpochMilli(),
                    previousEndMillis = start.toInstant().minusMillis(1).toEpochMilli()
                )
            }

            FixedInterval.MONTH -> {
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

    companion object {
        private const val ACTION_REFRESH = "com.example.widget.ACTION_REFRESH_FIXED"
    }
}


