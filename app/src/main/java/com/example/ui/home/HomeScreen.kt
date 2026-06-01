package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

    var showProfileMenu by remember { mutableStateOf(false) }
    var showAddForm by remember { mutableStateOf(false) }
    var showSmsEditForm by remember { mutableStateOf(false) }
    var expandedMonthDropdown by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var editingUntrackedExpense by remember { mutableStateOf<Expense?>(null) }
    var untrackedSectionExpanded by remember { mutableStateOf(true) }

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

                // Month Dropdown selector
                item(key = "month_selector") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .background(DarkSurface, RoundedCornerShape(14.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                                .clickable { expandedMonthDropdown = true }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Month icon", tint = AccentYellow, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = selectedMonth?.displayLabel ?: "All Historic Expenses",
                                    color = LightText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "arrow", tint = MutedText)
                        }

                        DropdownMenu(
                            expanded = expandedMonthDropdown,
                            onDismissRequest = { expandedMonthDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(DarkSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Time", color = LightText) },
                                onClick = {
                                    viewModel.selectMonth(null)
                                    expandedMonthDropdown = false
                                }
                            )
                            viewModel.availableMonths.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.displayLabel, color = LightText) },
                                    onClick = {
                                        viewModel.selectMonth(m)
                                        expandedMonthDropdown = false
                                    }
                                )
                            }
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

                // Section title: History header
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

                        if (expenses.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { onNavigateToGraph(selectedMonth?.queryValue ?: "all") }
                                    .testTag("view_graph_button")
                            ) {
                                Text(
                                    text = "VIEW ANALYTICS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AccentYellow,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = "Graph icon",
                                    tint = AccentYellow,
                                    modifier = Modifier.size(16.dp)
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

