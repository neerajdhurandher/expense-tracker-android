package com.example.ui.home

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Category
import com.example.data.model.Expense
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
    val categories by viewModel.categories.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    var showProfileMenu by remember { mutableStateOf(false) }
    var showAddForm by remember { mutableStateOf(false) }
    var expandedMonthDropdown by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
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

                // Month Dropdown selector
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

                // Summary Card styled as premium Indigo background banner
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

                // Section title: Transactions List & View Graph Link
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

                // List of Expenses with swipe to delete
                if (expenses.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 60.dp),
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
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items = expenses, key = { it.id }) { expense ->
                            ExpenseItemRow(
                                expense = expense,
                                categories = categories,
                                onDelete = {
                                    viewModel.deleteExpense(expense)
                                    // Trigger snackbar undo action
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
                        }
                    }
                }
            }

            // Slide up Add Expense bottom dialog
            if (showAddForm) {
                Dialog(
                    onDismissRequest = { showAddForm = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { showAddForm = false },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = false) {}
                        ) {
                            ExpenseFormSheet(
                                categories = categories,
                                onSave = { name, amount, category ->
                                    viewModel.addManualExpense(name, amount, category)
                                    showAddForm = false
                                },
                                onDismiss = { showAddForm = false }
                            )
                        }
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
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
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
        },
        content = {
            val matchingColorHex = categories.find { it.name == expense.category }?.color ?: "#ADB5BD"
            val categoryColor = Color(android.graphics.Color.parseColor(matchingColorHex))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .testTag("expense_item_${expense.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Small color badge containing category letters
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
