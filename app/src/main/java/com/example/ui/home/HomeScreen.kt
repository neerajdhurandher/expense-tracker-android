package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.PaymentSource
import com.example.ui.components.ExpenseFormSheet
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LightText
import com.example.ui.theme.MutedText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userName: String,
    userEmail: String,
    onNavigateToGraph: (String) -> Unit,
    onNavigateToCategories: () -> Unit,
    onSignOut: () -> Unit
) {
    val expenses by viewModel.expensesList.collectAsState()
    val untrackedExpenses by viewModel.untrackedExpenses.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val paymentSources by viewModel.paymentSources.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val pendingSmsExpense by viewModel.pendingSmsExpense.collectAsState()
    val historyFilter by viewModel.historyFilter.collectAsState()

    var showProfileMenu by remember { mutableStateOf(false) }
    var showAddForm by remember { mutableStateOf(false) }
    var showSmsEditForm by remember { mutableStateOf(false) }
    var showMonthSheet by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var editingUntrackedExpense by remember { mutableStateOf<Expense?>(null) }
    var untrackedSectionExpanded by remember { mutableStateOf(true) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Auto-show edit form when a pending SMS expense arrives from notification
    LaunchedEffect(pendingSmsExpense) {
        if (pendingSmsExpense != null) {
            showSmsEditForm = true
        }
    }

    // Calculated fields based on active state of expenses
    val totalAmount = expenses.sumOf { it.amount }
    val transactionCount = expenses.size
    val topCategory = if (expenses.isEmpty()) {
        "None"
    } else {
        expenses.groupBy { it.category }
            .maxByOrNull { (_, items) -> items.sumOf { it.amount } }?.key ?: "Other"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddForm = true },
                containerColor = AccentYellow,
                contentColor = DarkBg,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .testTag("add_expense_fab")
                    .padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add manual expense",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Header
                item(key = "header") {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom Header with User Profile actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Expenses",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = LightText,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Hello, $userName",
                                fontSize = 13.sp,
                                color = MutedText,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // Avatar Circle Action
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(AccentYellow.copy(alpha = 0.12f), CircleShape)
                                    .border(1.5.dp, AccentYellow, CircleShape)
                                    .clip(CircleShape)
                                    .clickable { showProfileMenu = true }
                                    .testTag("profile_avatar"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    color = AccentYellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            DropdownMenu(
                                expanded = showProfileMenu,
                                onDismissRequest = { showProfileMenu = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = "Category manage", tint = LightText) },
                                    text = { Text("Manage Categories", color = LightText) },
                                    onClick = {
                                        showProfileMenu = false
                                        onNavigateToCategories()
                                    },
                                    modifier = Modifier.testTag("menu_manage_categories")
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = "Sign out action", tint = LightText) },
                                    text = { Text("Sign Out", color = LightText) },
                                    onClick = {
                                        showProfileMenu = false
                                        onSignOut()
                                    },
                                    modifier = Modifier.testTag("menu_sign_out")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Month Dropdown + View Analytics row
                item(key = "month_selector") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Month selector button
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(DarkSurface, RoundedCornerShape(14.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                                .clickable { showMonthSheet = true }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Month icon", tint = AccentYellow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedMonth?.displayLabel ?: "All Time",
                                    color = LightText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "arrow", tint = MutedText, modifier = Modifier.size(20.dp))
                        }


                        // View Analytics button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(48.dp)
                                .background(DarkSurface, RoundedCornerShape(14.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                                .clickable { onNavigateToGraph(selectedMonth?.queryValue ?: "all") }
                                .padding(horizontal = 14.dp)
                                .testTag("view_graph_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Graph icon",
                                tint = AccentYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "View analytics",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LightText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Summary Card
                item(key = "summary_card") {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentYellow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "TOTAL SPENT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.8f),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "₹${String.format(Locale.getDefault(), "%,.2f", totalAmount)}",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.testTag("total_amount_text")
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Trend icon",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "TRANSACTIONS", fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "$transactionCount txns", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "TOP CATEGORY", fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = topCategory, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Untracked Expenses Section — only visible when there are pending items
                if (untrackedExpenses.isNotEmpty()) {
                    val untrackedTotal = untrackedExpenses.sumOf { it.amount }

                    // Section header
                    item(key = "untracked_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF7ED))
                                .border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(12.dp))
                                .clickable { untrackedSectionExpanded = !untrackedSectionExpanded }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("untracked_section_header"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Untracked",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Untracked Expenses",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${untrackedExpenses.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%,.0f", untrackedTotal)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (untrackedSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle",
                                    tint = Color(0xFF92400E),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Expanded untracked items — each as a LazyColumn item for proper scrolling
                    if (untrackedSectionExpanded) {
                        items(
                            items = untrackedExpenses,
                            key = { "untracked_${it.id}" }
                        ) { expense ->
                            UntrackedExpenseItem(
                                expense = expense,
                                categories = categories,
                                paymentSources = paymentSources,
                                onConfirm = { viewModel.confirmExpense(expense) },
                                onEdit = { editingUntrackedExpense = expense },
                                onDismiss = {
                                    viewModel.dismissUntrackedExpense(expense)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Dismissed: ${expense.name}",
                                            actionLabel = "UNDO",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoDeleteExpense()
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Bottom spacer after untracked section
                    item(key = "untracked_footer") {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Section title: History header with Filter
                item(key = "history_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "History",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(36.dp)
                                .background(DarkSurface, RoundedCornerShape(10.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                .clickable { showFilterSheet = true }
                                .padding(horizontal = 12.dp)
                                .testTag("filter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (historyFilter !is HistoryFilter.All) AccentYellow else MutedText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (historyFilter) {
                                    is HistoryFilter.All -> "Filter"
                                    is HistoryFilter.Saved -> "Saved"
                                    is HistoryFilter.ByCategory -> (historyFilter as HistoryFilter.ByCategory).categoryName
                                    is HistoryFilter.BySource -> (historyFilter as HistoryFilter.BySource).sourceName
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (historyFilter !is HistoryFilter.All) AccentYellow else LightText
                            )
                            if (historyFilter !is HistoryFilter.All) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear filter",
                                    tint = AccentYellow,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.setHistoryFilter(HistoryFilter.All) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Tracked expenses list OR empty state
                if (expenses.isEmpty()) {
                    item(key = "empty_state") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "No transactions icon",
                                tint = MutedText.copy(alpha = 0.25f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No recorded expenses",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Log tasks manually or wait for incoming transaction alerts.",
                                fontSize = 12.sp,
                                color = MutedText.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    items(items = expenses, key = { it.id }) { expense ->
                        ExpenseItemRow(
                            expense = expense,
                            categories = categories,
                            paymentSources = paymentSources,
                            isEditable = viewModel.isCurrentMonth(expense),
                            onEdit = {
                                if (viewModel.isCurrentMonth(expense)) {
                                    editingExpense = expense
                                }
                            },
                            onClick = {
                                if (viewModel.isCurrentMonth(expense)) {
                                    editingExpense = expense
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Only current month's expenses can be edited",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            onDelete = {
                                viewModel.deleteExpense(expense)
                                coroutineScope.launch {
                                    val snackbarResult = snackbarHostState.showSnackbar(
                                        message = "Deleted: ${expense.name}",
                                        actionLabel = "UNDO",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDeleteExpense()
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Bottom padding for FAB clearance
                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }

            // Add Expense ModalBottomSheet
            if (showAddForm) {
                val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showAddForm = false },
                    sheetState = addSheetState,
                    containerColor = DarkSurface,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = MutedText.copy(alpha = 0.4f)) }
                ) {
                    ExpenseFormSheet(
                        categories = categories,
                        paymentSources = paymentSources,
                        onSave = { name, amount, category, paymentSource ->
                            viewModel.addManualExpense(name, amount, category, paymentSource)
                            showAddForm = false
                        },
                        onDismiss = { showAddForm = false }
                    )
                }
            }

            // Edit Expense ModalBottomSheet
            editingExpense?.let { expense ->
                val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { editingExpense = null },
                    sheetState = editSheetState,
                    containerColor = DarkSurface,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = MutedText.copy(alpha = 0.4f)) }
                ) {
                    ExpenseFormSheet(
                        initialName = expense.name,
                        initialAmount = expense.amount,
                        initialCategory = expense.category,
                        initialPaymentSource = expense.paymentSource,
                        isEditMode = true,
                        categories = categories,
                        paymentSources = paymentSources,
                        onSave = { name, amount, category, paymentSource ->
                            viewModel.updateExpense(expense, name, amount, category, paymentSource)
                            editingExpense = null
                        },
                        onDismiss = { editingExpense = null }
                    )
                }
            }

            // SMS Expense edit ModalBottomSheet — triggered from notification "Edit" action
            if (showSmsEditForm && pendingSmsExpense != null) {
                val smsData = pendingSmsExpense!!
                val smsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = {
                        showSmsEditForm = false
                        viewModel.clearPendingSmsExpense()
                    },
                    sheetState = smsSheetState,
                    containerColor = DarkSurface,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = MutedText.copy(alpha = 0.4f)) }
                ) {
                    ExpenseFormSheet(
                        initialName = smsData.name,
                        initialAmount = smsData.amount,
                        initialCategory = smsData.category,
                        initialPaymentSource = smsData.paymentSource,
                        categories = categories,
                        paymentSources = paymentSources,
                        onSave = { name, amount, category, paymentSource ->
                            viewModel.savePendingSmsExpense(name, amount, category, paymentSource)
                            showSmsEditForm = false
                        },
                        onDismiss = {
                            showSmsEditForm = false
                            viewModel.clearPendingSmsExpense()
                        }
                    )
                }
            }

            // Edit Untracked Expense ModalBottomSheet
            editingUntrackedExpense?.let { expense ->
                val untrackedSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { editingUntrackedExpense = null },
                    sheetState = untrackedSheetState,
                    containerColor = DarkSurface,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = MutedText.copy(alpha = 0.4f)) }
                ) {
                    ExpenseFormSheet(
                        initialName = expense.name,
                        initialAmount = expense.amount,
                        initialCategory = expense.category,
                        initialPaymentSource = expense.paymentSource,
                        categories = categories,
                        paymentSources = paymentSources,
                        onSave = { name, amount, category, paymentSource ->
                            viewModel.confirmExpenseWithEdits(expense, name, amount, category, paymentSource)
                            editingUntrackedExpense = null
                        },
                        onDismiss = { editingUntrackedExpense = null }
                    )
                }
            }

            // Filter ModalBottomSheet
            if (showFilterSheet) {
                val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showFilterSheet = false },
                    sheetState = filterSheetState,
                    containerColor = DarkSurface,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = MutedText.copy(alpha = 0.4f)) }
                ) {
                    FilterSheetContent(
                        currentFilter = historyFilter,
                        categories = categories,
                        paymentSources = paymentSources,
                        onFilterSelected = { filter ->
                            viewModel.setHistoryFilter(filter)
                            showFilterSheet = false
                        },
                        onDismiss = { showFilterSheet = false }
                    )
                }
            }

            // Month Selector ModalBottomSheet
            if (showMonthSheet) {
                val monthSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showMonthSheet = false },
                    sheetState = monthSheetState,
                    containerColor = DarkSurface,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = MutedText.copy(alpha = 0.4f)) }
                ) {
                    MonthPickerSheetContent(
                        selectedMonth = selectedMonth,
                        availableMonths = viewModel.availableMonths,
                        onMonthSelected = { month ->
                            viewModel.selectMonth(month)
                            showMonthSheet = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UntrackedExpenseItem(
    expense: Expense,
    categories: List<Category>,
    paymentSources: List<PaymentSource>,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val matchingColorHex = categories.find { it.name == expense.category }?.color ?: "#ADB5BD"
    val categoryColor = Color(android.graphics.Color.parseColor(matchingColorHex))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(16.dp))
            .testTag("untracked_item_${expense.id}")
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
        ) {
            // Amber left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFF59E0B))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Top row: category icon + info + amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (expense.category) {
                                    "Food" -> Icons.Default.LunchDining
                                    "Travel" -> Icons.Default.DirectionsRun
                                    "Groceries" -> Icons.Default.ShoppingBasket
                                    "Shopping" -> Icons.Default.LocalMall
                                    "Bills" -> Icons.Default.FlashOn
                                    "Entertainment" -> Icons.Default.SportsEsports
                                    "Health" -> Icons.Default.LocalHospital
                                    else -> Icons.Default.Bookmark
                                },
                                contentDescription = "Category visual",
                                tint = categoryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = expense.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = expense.category,
                                    fontSize = 11.sp,
                                    color = categoryColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = " • ",
                                    fontSize = 11.sp,
                                    color = MutedText.copy(alpha = 0.5f)
                                )
                                val sourceColorHex = paymentSources.find { it.name == expense.paymentSource }?.color ?: "#4DABF7"
                                Text(
                                    text = expense.paymentSource,
                                    fontSize = 11.sp,
                                    color = Color(android.graphics.Color.parseColor(sourceColorHex)),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = " • ",
                                    fontSize = 11.sp,
                                    color = MutedText.copy(alpha = 0.5f)
                                )
                                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                Text(
                                    text = sdf.format(Date(expense.occurredAt)),
                                    fontSize = 11.sp,
                                    color = MutedText
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%,.2f", expense.amount)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFB45309)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "UNTRACKED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF59E0B),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("untracked_confirm_${expense.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Confirm", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentYellow),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("untracked_edit_${expense.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("untracked_dismiss_${expense.id}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dismiss", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseItemRow(
    expense: Expense,
    categories: List<Category>,
    paymentSources: List<PaymentSource> = emptyList(),
    isEditable: Boolean = false,
    onEdit: () -> Unit = {},
    onClick: () -> Unit = {},
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (isEditable) {
                        onEdit()
                    }
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = isEditable,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                val color = Color(0xFFFF5252)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(color)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Swipe to delete",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (direction == SwipeToDismissBoxValue.StartToEnd && isEditable) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AccentYellow)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Swipe to edit",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        content = {
            val matchingColorHex = categories.find { it.name == expense.category }?.color ?: "#ADB5BD"
            val categoryColor = Color(android.graphics.Color.parseColor(matchingColorHex))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onClick() }
                    .padding(16.dp)
                    .testTag("expense_item_${expense.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (expense.category) {
                                "Food" -> Icons.Default.LunchDining
                                "Travel" -> Icons.Default.DirectionsRun
                                "Groceries" -> Icons.Default.ShoppingBasket
                                "Shopping" -> Icons.Default.LocalMall
                                "Bills" -> Icons.Default.FlashOn
                                "Entertainment" -> Icons.Default.SportsEsports
                                "Health" -> Icons.Default.LocalHospital
                                else -> Icons.Default.Bookmark
                            },
                            contentDescription = "Category visual",
                            tint = categoryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = expense.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = expense.category,
                                fontSize = 11.sp,
                                color = categoryColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " • ",
                                fontSize = 11.sp,
                                color = MutedText.copy(alpha = 0.5f)
                            )
                            val sourceColorHex = paymentSources.find { it.name == expense.paymentSource }?.color ?: "#4DABF7"
                            Text(
                                text = expense.paymentSource,
                                fontSize = 11.sp,
                                color = Color(android.graphics.Color.parseColor(sourceColorHex)),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " • ",
                                fontSize = 11.sp,
                                color = MutedText.copy(alpha = 0.5f)
                            )
                            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            Text(
                                text = sdf.format(Date(expense.occurredAt)),
                                fontSize = 11.sp,
                                color = MutedText
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.2f", expense.amount)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentYellow
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = expense.source.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = MutedText.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    )
}

@Composable
fun MonthPickerSheetContent(
    selectedMonth: YearMonthItem?,
    availableMonths: List<YearMonthItem>,
    onMonthSelected: (YearMonthItem?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Select Month",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = LightText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // "All Time" option
        val allTimeSelected = selectedMonth == null
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (allTimeSelected) AccentYellow.copy(alpha = 0.10f) else Color.Transparent)
                .clickable { onMonthSelected(null) }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AllInclusive,
                contentDescription = "All Time",
                tint = if (allTimeSelected) AccentYellow else MutedText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "All Time",
                fontSize = 15.sp,
                fontWeight = if (allTimeSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (allTimeSelected) AccentYellow else LightText,
                modifier = Modifier.weight(1f)
            )
            if (allTimeSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = AccentYellow,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Month list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState())
        ) {
            availableMonths.forEach { month ->
                val isSelected = selectedMonth?.queryValue == month.queryValue
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AccentYellow.copy(alpha = 0.10f) else Color.Transparent)
                        .clickable { onMonthSelected(month) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Month",
                        tint = if (isSelected) AccentYellow else MutedText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = month.displayLabel,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) AccentYellow else LightText,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = AccentYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSheetContent(
    currentFilter: HistoryFilter,
    categories: List<Category>,
    paymentSources: List<PaymentSource>,
    onFilterSelected: (HistoryFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var showCategoryPicker by remember { mutableStateOf(currentFilter is HistoryFilter.ByCategory) }
    var showSourcePicker by remember { mutableStateOf(currentFilter is HistoryFilter.BySource) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        // Sheet title
        Text(
            text = "Filter Expenses",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = LightText,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // All option
        FilterOptionRow(
            label = "All",
            icon = Icons.Default.List,
            isSelected = currentFilter is HistoryFilter.All,
            onClick = { onFilterSelected(HistoryFilter.All) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Saved option
        FilterOptionRow(
            label = "Saved",
            icon = Icons.Default.BookmarkAdded,
            isSelected = currentFilter is HistoryFilter.Saved,
            onClick = { onFilterSelected(HistoryFilter.Saved) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // By Category option
        FilterOptionRow(
            label = "By Category",
            icon = Icons.Default.Category,
            isSelected = currentFilter is HistoryFilter.ByCategory,
            hasSubMenu = true,
            isExpanded = showCategoryPicker,
            onClick = {
                showCategoryPicker = !showCategoryPicker
                showSourcePicker = false
            }
        )

        // Category sub-picker
        if (showCategoryPicker) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 4.dp, bottom = 4.dp)
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                categories.forEach { category ->
                    val catColorHex = category.color
                    val catColor = Color(android.graphics.Color.parseColor(catColorHex))
                    val isSelected = currentFilter is HistoryFilter.ByCategory &&
                            (currentFilter as HistoryFilter.ByCategory).categoryName == category.name

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AccentYellow.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onFilterSelected(HistoryFilter.ByCategory(category.name)) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(catColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = category.name,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AccentYellow else LightText,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = AccentYellow,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // By Source option
        FilterOptionRow(
            label = "By Source",
            icon = Icons.Default.AccountBalanceWallet,
            isSelected = currentFilter is HistoryFilter.BySource,
            hasSubMenu = true,
            isExpanded = showSourcePicker,
            onClick = {
                showSourcePicker = !showSourcePicker
                showCategoryPicker = false
            }
        )

        // Source sub-picker
        if (showSourcePicker) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 4.dp, bottom = 4.dp)
            ) {
                paymentSources.forEach { source ->
                    val sourceColor = Color(android.graphics.Color.parseColor(source.color))
                    val isSelected = currentFilter is HistoryFilter.BySource &&
                            (currentFilter as HistoryFilter.BySource).sourceName == source.name

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AccentYellow.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onFilterSelected(HistoryFilter.BySource(source.name)) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(sourceColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = source.name,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AccentYellow else LightText,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = AccentYellow,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Clear filter button (only when a filter is active)
        if (currentFilter !is HistoryFilter.All) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onFilterSelected(HistoryFilter.All) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(Icons.Default.FilterListOff, contentDescription = "Clear filter", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Filter", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FilterOptionRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    hasSubMenu: Boolean = false,
    isExpanded: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) AccentYellow.copy(alpha = 0.10f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) AccentYellow else MutedText,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) AccentYellow else LightText,
            modifier = Modifier.weight(1f)
        )
        if (hasSubMenu) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Expand",
                tint = MutedText,
                modifier = Modifier.size(20.dp)
            )
        } else if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = AccentYellow,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

