package com.example.ui.graph

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.home.HomeViewModel
import com.example.data.model.BudgetSummary
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LightText
import com.example.ui.theme.MutedText
import java.util.*

data class CategoryShare(
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Float,
    val colorHex: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    val expenses by viewModel.expensesList.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val paymentSources by viewModel.paymentSources.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val budgetSummary by viewModel.budgetSummary.collectAsState()

    var viewMode by remember { mutableIntStateOf(0) } // 0 = Category, 1 = Source, 2 = Budget

    // Calculate totals
    val totalAmount = expenses.sumOf { it.amount }

    val shares = remember(expenses, categories, paymentSources, viewMode) {
        if (expenses.isEmpty()) {
            emptyList()
        } else if (viewMode == 0) {
            // Category-wise grouping
            expenses.groupBy { it.category }
                .map { (catName, items) ->
                    val catTotal = items.sumOf { it.amount }
                    val matchingColorHex = categories.find { it.name == catName }?.color ?: "#ADB5BD"
                    CategoryShare(
                        categoryName = catName,
                        totalAmount = catTotal,
                        percentage = if (totalAmount > 0) ((catTotal / totalAmount) * 100f).toFloat() else 0f,
                        colorHex = matchingColorHex
                    )
                }.sortedByDescending { it.totalAmount }
        } else {
            // Source-wise grouping
            expenses.groupBy { it.paymentSource }
                .map { (sourceName, items) ->
                    val sourceTotal = items.sumOf { it.amount }
                    val colorHex = paymentSources.find { it.name == sourceName }?.color ?: "#ADB5BD"
                    CategoryShare(
                        categoryName = sourceName,
                        totalAmount = sourceTotal,
                        percentage = if (totalAmount > 0) ((sourceTotal / totalAmount) * 100f).toFloat() else 0f,
                        colorHex = colorHex
                    )
                }.sortedByDescending { it.totalAmount }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedMonth != null) "${selectedMonth!!.displayLabel} Analytics" else "All Time Analytics",
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button_graph")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = LightText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (expenses.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No analytics data available",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Log some expenses to view dynamic category share diagrams.",
                        fontSize = 12.sp,
                        color = MutedText.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))

                // Toggle: BY CATEGORY | BY SOURCE | BUDGET
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val toggleLabels = listOf("BY CATEGORY", "BY SOURCE", "BUDGET")
                    toggleLabels.forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (viewMode == index) AccentYellow else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewMode = index }
                                .padding(vertical = 10.dp)
                                .then(if (index == 2) Modifier.testTag("graph_budget_toggle") else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewMode == index) DarkBg else MutedText,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (viewMode == 2) {
                    // Budget view mode
                    BudgetAnalyticsContent(budgetSummary = budgetSummary)
                } else {
                    // Category / Source view mode (existing)

                    // High-Fidelity Donut Chart component with animations
                    DonutChart(
                        shares = shares,
                        totalAmount = totalAmount,
                        modifier = Modifier
                            .size(240.dp)
                            .padding(16.dp)
                            .testTag("donut_chart")
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Section title
                    Text(
                        text = if (viewMode == 0) "Category Breakdown" else "Source Breakdown",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    // List of shares
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(shares) { share ->
                            val catColor = Color(android.graphics.Color.parseColor(share.colorHex))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurface, RoundedCornerShape(14.dp))
                                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(catColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = share.categoryName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LightText
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.1f%% of overall spend", share.percentage),
                                            fontSize = 11.sp,
                                            color = MutedText
                                        )
                                    }
                                }

                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%,.2f", share.totalAmount)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AccentYellow
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    shares: List<CategoryShare>,
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    val animateStroke = remember { Animatable(0f) }

    LaunchedEffect(shares) {
        animateStroke.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = -90f
            val strokeWidth = 32.dp.toPx()

            shares.forEach { share ->
                val sweepAngle = (share.percentage * 360f / 100f) * animateStroke.value
                val sliceColor = Color(android.graphics.Color.parseColor(share.colorHex))
                drawArc(
                    color = sliceColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
            }
        }

        // Inside details
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "₹${String.format(Locale.getDefault(), "%,.0f", totalAmount)}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = AccentYellow
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "total spent",
                fontSize = 11.sp,
                color = MutedText,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun ColumnScope.BudgetAnalyticsContent(budgetSummary: BudgetSummary?) {
    val summary = budgetSummary

    if (summary == null || summary.totalBudget <= 0) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No budget data", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MutedText)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Set budgets for your payment sources to see budget analytics.",
                fontSize = 12.sp,
                color = MutedText.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val totalRemaining = summary.totalRemaining
    val budgetShares = summary.sourceBudgets.filter { it.budgetAmount > 0 }.map { status ->
        val pct = if (summary.totalBudget > 0) ((status.budgetAmount / summary.totalBudget) * 100f).toFloat() else 0f
        CategoryShare(
            categoryName = status.sourceName,
            totalAmount = status.spentAmount,
            percentage = pct,
            colorHex = status.sourceColor
        )
    }

    // Donut chart showing budget allocation per source
    DonutChart(
        shares = budgetShares,
        totalAmount = summary.totalBudget,
        modifier = Modifier
            .size(240.dp)
            .padding(16.dp)
    )

    // Center label override
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = if (totalRemaining >= 0) "₹${String.format(Locale.getDefault(), "%,.0f", totalRemaining)} remaining"
        else "Over by ₹${String.format(Locale.getDefault(), "%,.0f", -totalRemaining)}",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = if (totalRemaining >= 0) Color(0xFF22C55E) else ErrorRed,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Budget Breakdown",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = LightText,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )

    // Budget vs Actual bars
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().weight(1f)
    ) {
        items(summary.sourceBudgets.filter { it.budgetAmount > 0 }) { status ->
            val sourceColor = try {
                Color(android.graphics.Color.parseColor(status.sourceColor))
            } catch (_: Exception) { AccentYellow }

            val utilization = if (status.budgetAmount > 0) (status.spentAmount / status.budgetAmount).toFloat() else 0f
            val isOver = utilization > 1f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(sourceColor, CircleShape))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(status.sourceName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LightText)
                    }
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.0f", status.spentAmount)} / ₹${String.format(Locale.getDefault(), "%,.0f", status.budgetAmount)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOver) ErrorRed else LightText
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { minOf(utilization, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isOver) ErrorRed else sourceColor,
                    trackColor = CardBorder,
                )

                if (isOver) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Over budget by ₹${String.format(Locale.getDefault(), "%,.0f", -status.remainingAmount)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                }
            }
        }
    }
}

