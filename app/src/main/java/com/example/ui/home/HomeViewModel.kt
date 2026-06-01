package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.PaymentSource
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

class HomeViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentSourceRepository: PaymentSourceRepository
) : ViewModel() {

    // Generate dynamic list of the last 12 months for dropdown
    val availableMonths: List<YearMonthItem> = createAvailableMonthsList()

    private val _selectedMonth = MutableStateFlow<YearMonthItem?>(availableMonths.firstOrNull())
    val selectedMonth: StateFlow<YearMonthItem?> = _selectedMonth.asStateFlow()

    // Only tracked expenses for history & totals
    @OptIn(ExperimentalCoroutinesApi::class)
    val expensesList: StateFlow<List<Expense>> = _selectedMonth
        .flatMapLatest { monthItem ->
            if (monthItem == null) {
                expenseRepository.allTrackedExpenses
            } else {
                expenseRepository.getTrackedExpensesByMonth(monthItem.queryValue)
            }
        }
        .stateIn(
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

    // Pending SMS expense from notification "Edit" action
    data class PendingSmsExpense(
        val name: String,
        val amount: Double,
        val category: String,
        val rawSms: String?,
        val sender: String?,
        val occurredAt: Long,
        val paymentSource: String = "UPI",
        val expenseId: Long = -1L // Links to the pre-inserted untracked DB row
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
            // Update existing untracked expense and mark tracked
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
            // Fallback: insert as new (backward compat)
            addParsedSmsExpense(name, amount, category, pending.rawSms, pending.sender, pending.occurredAt, paymentSource)
        }
        _pendingSmsExpense.value = null
    }

    // --- Untracked expense actions ---

    /** Confirm an untracked expense as-is (mark tracked) */
    fun confirmExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.markAsTracked(expense.id)
        }
    }

    /** Confirm an untracked expense with edits */
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

    /** Dismiss (delete) an untracked expense */
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
        private val paymentSourceRepo: PaymentSourceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(expenseRepo, categoryRepo, paymentSourceRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
