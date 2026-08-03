package com.example.widget

import com.example.R

class MonthExpenseWidgetProvider : FixedIntervalExpenseWidgetProvider(
    interval = FixedInterval.MONTH,
    titleRes = R.string.widget_title_expenses_month,
    layoutRes = R.layout.widget_expense_summary_month_small
)

