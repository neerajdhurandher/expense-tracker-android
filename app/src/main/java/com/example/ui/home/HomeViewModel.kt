package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.repo.CategoryRepository
import com.example.data.repo.ExpenseRepository
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
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // Generate dynamic list of the last 12 months for dropdown
    val availableMonths: List<YearMonthItem> = createAvailableMonthsList()

    private val _selectedMonth = MutableStateFlow<YearMonthItem?>(availableMonths.firstOrNull())
    val selectedMonth: StateFlow<YearMonthItem?> = _selectedMonth.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val expensesList: StateFlow<List<Expense>> = _selectedMonth
        .flatMapLatest { monthItem ->
            if (monthItem == null) {
                expenseRepository.allExpenses
            } else {
                expenseRepository.getExpensesByMonth(monthItem.queryValue)
            }
        }
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

    // Keeps track of the last deleted item for Undo action
    private var recentlyDeletedExpense: Expense? = null

    fun selectMonth(month: YearMonthItem?) {
        _selectedMonth.value = month
    }

    fun addManualExpense(name: String, amount: Double, category: String) {
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
                yearMonth = yearMonthStr
            )
            expenseRepository.insertExpense(expense)
        }
    }

    fun addParsedSmsExpense(name: String, amount: Double, category: String, rawSms: String?, sender: String?, occurredAt: Long) {
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
                yearMonth = yearMonthStr
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
        private val categoryRepo: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(expenseRepo, categoryRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
