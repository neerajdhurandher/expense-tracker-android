package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.BudgetSummary
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.PaymentSource
import com.example.data.model.SourceBudgetStatus
import com.example.data.repo.BudgetRepository
import com.example.data.repo.CategoryRepository
import com.example.data.repo.ExpenseRepository
import com.example.data.repo.PaymentSourceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class YearMonthItem(
    val queryValue: String,    // Format: "YYYY-MM"
    val displayLabel: String   // Format: "Month YYYY"
)

sealed class HistoryFilter {
    data object All : HistoryFilter()
    data object Saved : HistoryFilter()
    data class ByCategory(val categoryName: String) : HistoryFilter()
    data class BySource(val sourceName: String) : HistoryFilter()
}

class HomeViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentSourceRepository: PaymentSourceRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    // Generate dynamic list of the last 12 months for dropdown
    val availableMonths: List<YearMonthItem> = createAvailableMonthsList()

    private val _selectedMonth = MutableStateFlow<YearMonthItem?>(availableMonths.firstOrNull())
    val selectedMonth: StateFlow<YearMonthItem?> = _selectedMonth.asStateFlow()

    private val _historyFilter = MutableStateFlow<HistoryFilter>(HistoryFilter.All)
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

    fun setHistoryFilter(filter: HistoryFilter) {
        _historyFilter.value = filter
    }

    // Only tracked expenses for history & totals, combined with active filter
    @OptIn(ExperimentalCoroutinesApi::class)
    val expensesList: StateFlow<List<Expense>> = combine(_selectedMonth, _historyFilter) { month, filter ->
        Pair(month, filter)
    }.flatMapLatest { (monthItem, filter) ->
        val baseFlow = if (monthItem == null) {
            expenseRepository.allTrackedExpenses
        } else {
            expenseRepository.getTrackedExpensesByMonth(monthItem.queryValue)
        }
        baseFlow.map { expenses ->
            when (filter) {
                is HistoryFilter.All -> expenses
                is HistoryFilter.Saved -> expenses
                is HistoryFilter.ByCategory -> expenses.filter { it.category == filter.categoryName }
                is HistoryFilter.BySource -> expenses.filter { it.paymentSource == filter.sourceName }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // All untracked expenses (no month filter — show all pending)
    val untrackedExpenses: StateFlow<List<Expense>> = expenseRepository.allUntrackedExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val paymentSources: StateFlow<List<PaymentSource>> = paymentSourceRepository.allPaymentSources
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Keeps track of the last deleted item for Undo action
    private var recentlyDeletedExpense: Expense? = null

    // Current month in "YYYY-MM" format for edit eligibility check
    private val currentYearMonth: String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    // ═══════════════════════════════════════════════════════════════
    // Budget State
    // ═══════════════════════════════════════════════════════════════

    // Carry-over cache — refreshed when selected month changes
    private val _carryOverAmounts = MutableStateFlow<Map<String, Double>>(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetSummary: StateFlow<BudgetSummary?> = _selectedMonth
        .flatMapLatest { monthItem ->
            val yearMonth = monthItem?.queryValue ?: currentYearMonth
            // Refresh carry-over when month changes
            viewModelScope.launch {
                _carryOverAmounts.value = try {
                    budgetRepository.getCarryOverFromPreviousMonth(yearMonth)
                } catch (_: Exception) { emptyMap() }
            }

            combine(
                budgetRepository.getBudgetsByMonth(yearMonth),
                budgetRepository.getSpendingBySourceForMonth(yearMonth),
                paymentSourceRepository.allPaymentSources,
                _carryOverAmounts
            ) { budgets, spending, sources, carryOvers ->
                if (budgets.isEmpty() && sources.isEmpty()) return@combine null

                val spendingMap = spending.associate { it.paymentSource to it.total }
                val budgetMap = budgets.associateBy { it.sourceName }

                val sourceBudgetStatuses = sources.map { source ->
                    val budget = budgetMap[source.name]
                    val baseBudget = budget?.amount ?: 0.0
                    val carryOverAmount = if (budget?.carryOver == true) carryOvers[source.name] ?: 0.0 else 0.0
                    val effectiveBudget = baseBudget + carryOverAmount
                    val spent = spendingMap[source.name] ?: 0.0
                    val remaining = effectiveBudget - spent

                    SourceBudgetStatus(
                        sourceName = source.name,
                        sourceColor = source.color,
                        budgetAmount = effectiveBudget,
                        spentAmount = spent,
                        remainingAmount = remaining,
                        carryOver = budget?.carryOver ?: false,
                        carryOverAmount = carryOverAmount
                    )
                }

                val totalBudget = sourceBudgetStatuses.sumOf { it.budgetAmount }
                val totalSpent = sourceBudgetStatuses.sumOf { it.spentAmount }
                val totalRemaining = totalBudget - totalSpent
                val utilization = if (totalBudget > 0) (totalSpent / totalBudget).toFloat() else 0f

                BudgetSummary(
                    totalBudget = totalBudget,
                    totalSpent = totalSpent,
                    totalRemaining = totalRemaining,
                    utilizationPercent = utilization,
                    sourceBudgets = sourceBudgetStatuses
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Set budget for a source in the currently selected month */
    fun setBudget(sourceName: String, amount: Double, carryOver: Boolean) {
        val yearMonth = _selectedMonth.value?.queryValue ?: currentYearMonth
        viewModelScope.launch {
            budgetRepository.setBudget(sourceName, yearMonth, amount, carryOver)
        }
    }

    /** Set budget for a source in a specific month */
    fun setBudgetForMonth(sourceName: String, yearMonth: String, amount: Double, carryOver: Boolean) {
        viewModelScope.launch {
            budgetRepository.setBudget(sourceName, yearMonth, amount, carryOver)
        }
    }

    /** Copy all budgets from last month to current month */
    fun copyBudgetsFromLastMonth() {
        val yearMonth = _selectedMonth.value?.queryValue ?: currentYearMonth
        val prevMonth = budgetRepository.getPreviousMonth(yearMonth)
        viewModelScope.launch {
            budgetRepository.copyBudgetsFromMonth(prevMonth, yearMonth)
        }
    }

    /** Copy budgets from previous month to a specific month */
    fun copyBudgetsFromLastMonthTo(yearMonth: String) {
        val prevMonth = budgetRepository.getPreviousMonth(yearMonth)
        viewModelScope.launch {
            budgetRepository.copyBudgetsFromMonth(prevMonth, yearMonth)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Pending SMS Expense (existing)
    // ══════════════════════════════════════════════════════════════

    data class PendingSmsExpense(
        val name: String,
        val amount: Double,
        val category: String,
        val rawSms: String?,
        val sender: String?,
        val occurredAt: Long,
        val paymentSource: String = "UPI",
        val expenseId: Long = -1L
    )

    private val _pendingSmsExpense = MutableStateFlow<PendingSmsExpense?>(null)
    val pendingSmsExpense: StateFlow<PendingSmsExpense?> = _pendingSmsExpense.asStateFlow()

    fun setPendingSmsExpense(name: String, amount: Double, category: String, rawSms: String?, sender: String?, occurredAt: Long, paymentSource: String = "UPI", expenseId: Long = -1L) {
        _pendingSmsExpense.value = PendingSmsExpense(name, amount, category, rawSms, sender, occurredAt, paymentSource, expenseId)
    }

    fun clearPendingSmsExpense() {
        _pendingSmsExpense.value = null
    }

    fun savePendingSmsExpense(name: String, amount: Double, category: String, paymentSource: String) {
        val pending = _pendingSmsExpense.value ?: return
        if (pending.expenseId > 0) {
            viewModelScope.launch {
                val existing = expenseRepository.getExpenseById(pending.expenseId)
                if (existing != null) {
                    val updated = existing.copy(
                        name = name,
                        amount = amount,
                        category = category,
                        paymentSource = paymentSource,
                        isTracked = true
                    )
                    expenseRepository.updateExpense(updated)
                }
            }
        } else {
            addParsedSmsExpense(name, amount, category, pending.rawSms, pending.sender, pending.occurredAt, paymentSource)
        }
        _pendingSmsExpense.value = null
    }

    // --- Untracked expense actions ---

    fun confirmExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.markAsTracked(expense.id)
        }
    }

    fun confirmExpenseWithEdits(expense: Expense, name: String, amount: Double, category: String, paymentSource: String) {
        viewModelScope.launch {
            val updated = expense.copy(
                name = name,
                amount = amount,
                category = category,
                paymentSource = paymentSource,
                isTracked = true
            )
            expenseRepository.updateExpense(updated)
        }
    }

    fun dismissUntrackedExpense(expense: Expense) {
        viewModelScope.launch {
            recentlyDeletedExpense = expense
            expenseRepository.deleteExpenseById(expense.id)
        }
    }

    fun isCurrentMonth(expense: Expense): Boolean {
        return expense.yearMonth == currentYearMonth
    }

    fun selectMonth(month: YearMonthItem?) {
        _selectedMonth.value = month
    }

    fun addManualExpense(name: String, amount: Double, category: String, paymentSource: String = "UPI") {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val yearMonthStr = sdf.format(Date(now))

            val expense = Expense(
                name = name,
                amount = amount,
                category = category,
                source = "manual",
                occurredAt = now,
                createdAt = now,
                yearMonth = yearMonthStr,
                paymentSource = paymentSource
            )
            expenseRepository.insertExpense(expense)
        }
    }

    fun addParsedSmsExpense(name: String, amount: Double, category: String, rawSms: String?, sender: String?, occurredAt: Long, paymentSource: String = "UPI") {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val yearMonthStr = sdf.format(Date(occurredAt))

            val expense = Expense(
                name = name,
                amount = amount,
                category = category,
                source = "sms",
                rawSms = rawSms,
                sender = sender,
                occurredAt = occurredAt,
                createdAt = System.currentTimeMillis(),
                yearMonth = yearMonthStr,
                paymentSource = paymentSource
            )
            expenseRepository.insertExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            recentlyDeletedExpense = expense
            expenseRepository.deleteExpenseById(expense.id)
        }
    }

    fun undoDeleteExpense() {
        val expenseToRestore = recentlyDeletedExpense ?: return
        viewModelScope.launch {
            expenseRepository.insertExpense(expenseToRestore)
            recentlyDeletedExpense = null
        }
    }

    fun addCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.insertCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    fun addPaymentSource(source: PaymentSource) {
        viewModelScope.launch {
            paymentSourceRepository.insertPaymentSource(source)
        }
    }

    fun deletePaymentSource(source: PaymentSource) {
        viewModelScope.launch {
            paymentSourceRepository.deletePaymentSource(source)
        }
    }

    fun updateExpense(expense: Expense, name: String, amount: Double, category: String, paymentSource: String) {
        viewModelScope.launch {
            val updated = expense.copy(
                name = name,
                amount = amount,
                category = category,
                paymentSource = paymentSource
            )
            expenseRepository.updateExpense(updated)
        }
    }

    private fun createAvailableMonthsList(): List<YearMonthItem> {
        val list = mutableListOf<YearMonthItem>()
        val calendar = Calendar.getInstance()
        val sdfQuery = SimpleDateFormat("yyyy-MM", Locale.US)
        val sdfDisplay = SimpleDateFormat("MMMM yyyy", Locale.US)

        for (i in 0 until 12) {
            list.add(
                YearMonthItem(
                    queryValue = sdfQuery.format(calendar.time),
                    displayLabel = sdfDisplay.format(calendar.time)
                )
            )
            calendar.add(Calendar.MONTH, -1)
        }
        return list
    }

    class Factory(
        private val expenseRepo: ExpenseRepository,
        private val categoryRepo: CategoryRepository,
        private val paymentSourceRepo: PaymentSourceRepository,
        private val budgetRepo: BudgetRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(expenseRepo, categoryRepo, paymentSourceRepo, budgetRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
