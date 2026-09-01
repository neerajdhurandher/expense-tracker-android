package com.example.widget

import com.example.R

class TodayExpenseWidgetProvider : FixedIntervalExpenseWidgetProvider(
    interval = FixedInterval.TODAY,
    titleRes = R.string.widget_title_expenses_today,
    layoutRes = R.layout.widget_expense_summary_today_small
)

