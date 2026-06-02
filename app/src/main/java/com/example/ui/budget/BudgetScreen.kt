package com.example.ui.budget

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BudgetSummary
import com.example.data.model.SourceBudgetStatus
import com.example.ui.home.HomeViewModel
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    val budgetSummary by viewModel.budgetSummary.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val context = LocalContext.current

    val monthLabel = selectedMonth?.displayLabel ?: "All Time"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Budget — $monthLabel",
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("budget_detail_back")) {
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
        val summary = budgetSummary

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "top_spacer") {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Overall Summary Card
            item(key = "overall_summary") {
                OverallBudgetCard(summary = summary)
            }

            // Copy from last month button
            item(key = "copy_button") {
                OutlinedButton(
                    onClick = {
                        viewModel.copyBudgetsFromLastMonth()
                        Toast.makeText(context, "Budgets copied from last month", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentYellow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("budget_copy_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy from last month", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Section header
            item(key = "source_header") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SOURCE BUDGETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedText,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Per-source budget cards
            val sources = summary?.sourceBudgets ?: emptyList()
            if (sources.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "No budgets",
                            tint = MutedText.copy(alpha = 0.3f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No budgets set",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set budgets from Sources & Budget in Settings.",
                            fontSize = 12.sp,
                            color = MutedText.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(sources, key = { it.sourceName }) { sourceBudget ->
                    SourceBudgetCard(
                        status = sourceBudget,
                        onToggleCarryOver = { newValue ->
                            viewModel.setBudget(sourceBudget.sourceName, sourceBudget.budgetAmount - sourceBudget.carryOverAmount, newValue)
                        }
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun OverallBudgetCard(summary: BudgetSummary?) {
    val totalBudget = summary?.totalBudget ?: 0.0
    val totalSpent = summary?.totalSpent ?: 0.0
    val totalRemaining = summary?.totalRemaining ?: 0.0
    val utilization = summary?.utilizationPercent ?: 0f

    val progressColor = when {
        utilization > 1f -> ErrorRed
        utilization > 0.8f -> Color(0xFFF97316)
        utilization > 0.6f -> Color(0xFFF59E0B)
        else -> Color(0xFF22C55E)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("OVERALL BUDGET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedText, letterSpacing = 1.sp)
                Text(
                    text = "₹${String.format(Locale.getDefault(), "%,.0f", totalBudget)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentYellow
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { minOf(utilization, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .testTag("budget_progress_bar"),
                color = progressColor,
                trackColor = CardBorder,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${String.format(Locale.getDefault(), "%.0f", minOf(utilization * 100, 999f))}% used",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = CardBorder)

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("SPENT", fontSize = 10.sp, color = MutedText, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.0f", totalSpent)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("REMAINING", fontSize = 10.sp, color = MutedText, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (totalRemaining >= 0) "₹${String.format(Locale.getDefault(), "%,.0f", totalRemaining)}"
                        else "-₹${String.format(Locale.getDefault(), "%,.0f", -totalRemaining)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalRemaining < 0) ErrorRed else Color(0xFF22C55E)
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceBudgetCard(
    status: SourceBudgetStatus,
    onToggleCarryOver: (Boolean) -> Unit
) {
    val sourceColor = try {
        Color(android.graphics.Color.parseColor(status.sourceColor))
    } catch (_: Exception) {
        AccentYellow
    }

    val utilization = if (status.budgetAmount > 0) (status.spentAmount / status.budgetAmount).toFloat() else 0f
    val progressColor = when {
        utilization > 1f -> ErrorRed
        utilization > 0.8f -> Color(0xFFF97316)
        utilization > 0.6f -> Color(0xFFF59E0B)
        else -> Color(0xFF22C55E)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .testTag("budget_source_card_${status.sourceName}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: source name + color dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(sourceColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = status.sourceName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Budget / Spent / Remaining row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Budget", fontSize = 10.sp, color = MutedText, fontWeight = FontWeight.Bold)
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.0f", status.budgetAmount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Spent", fontSize = 10.sp, color = MutedText, fontWeight = FontWeight.Bold)
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.0f", status.spentAmount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Remaining", fontSize = 10.sp, color = MutedText, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (status.remainingAmount >= 0) "₹${String.format(Locale.getDefault(), "%,.0f", status.remainingAmount)}"
                        else "-₹${String.format(Locale.getDefault(), "%,.0f", -status.remainingAmount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (status.remainingAmount < 0) ErrorRed else Color(0xFF22C55E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            if (status.budgetAmount > 0) {
                LinearProgressIndicator(
                    progress = { minOf(utilization, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = CardBorder,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${String.format(Locale.getDefault(), "%.1f", minOf(utilization * 100, 999f))}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(color = CardBorder)

            Spacer(modifier = Modifier.height(8.dp))

            // Carry-over toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Carry-over unspent",
                    fontSize = 13.sp,
                    color = MutedText,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = status.carryOver,
                    onCheckedChange = onToggleCarryOver,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentYellow,
                        uncheckedThumbColor = MutedText,
                        uncheckedTrackColor = CardBorder
                    ),
                    modifier = Modifier.testTag("budget_carryover_toggle_${status.sourceName}")
                )
            }

            // Carry-over info
            if (status.carryOver && status.carryOverAmount > 0) {
                Text(
                    text = "↳ Includes ₹${String.format(Locale.getDefault(), "%,.0f", status.carryOverAmount)} carry-over",
                    fontSize = 11.sp,
                    color = Color(0xFF14B8A6),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

