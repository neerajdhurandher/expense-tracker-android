package com.example.ui.sourcebudget

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentSource
import com.example.data.model.SourceBudgetStatus
import com.example.ui.home.HomeViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceBudgetScreen(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    val paymentSources by viewModel.paymentSources.collectAsState()
    val budgetSummary by viewModel.budgetSummary.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Current month for budget setting
    val currentYearMonth = remember {
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    }
    val currentMonthLabel = remember {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
    }

    // New source form state
    var newSourceName by remember { mutableStateOf("") }
    var newSmartKeywords by remember { mutableStateOf("") }
    val colorPresets = listOf(
        "#FF6B6B", "#4DABF7", "#51CF66", "#FCC419",
        "#BE4BDB", "#FF922B", "#20C997", "#ADB5BD",
        "#FF8787", "#74C0FC", "#63E6BE", "#FFD43B"
    )
    var selectedColorHex by remember { mutableStateOf(colorPresets[4]) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sources & Budget", fontWeight = FontWeight.Bold, color = LightText) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("source_budget_back")) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Add new source form
            item(key = "add_source_form") {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "ADD NEW SOURCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentYellow,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = newSourceName,
                            onValueChange = { newSourceName = it },
                            label = { Text("Source Name") },
                            placeholder = { Text("e.g. HDFC Savings, Paytm Wallet") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentYellow,
                                focusedLabelColor = AccentYellow,
                                cursorColor = AccentYellow,
                                unfocusedBorderColor = CardBorder,
                                unfocusedTextColor = LightText,
                                focusedTextColor = LightText
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("source_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = newSmartKeywords,
                            onValueChange = { newSmartKeywords = it },
                            label = { Text("Smart Keywords") },
                            placeholder = { Text("e.g. hdfc, hdfcbk, xx1234 (comma separated)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentYellow,
                                focusedLabelColor = AccentYellow,
                                cursorColor = AccentYellow,
                                unfocusedBorderColor = CardBorder,
                                unfocusedTextColor = LightText,
                                focusedTextColor = LightText
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("source_keywords_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Keywords help auto-detect this source from SMS messages",
                            fontSize = 11.sp,
                            color = MutedText.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Pick Theme Accent Color", fontSize = 13.sp, color = MutedText, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(colorPresets) { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            width = if (selectedColorHex == hex) 3.dp else 0.dp,
                                            color = if (selectedColorHex == hex) LightText else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorHex = hex }
                                        .testTag("source_color_chip_$hex"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedColorHex == hex) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val nameClean = newSourceName.trim()
                                if (nameClean.isEmpty()) {
                                    Toast.makeText(context, "Source name cannot be empty", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (paymentSources.any { it.name.lowercase() == nameClean.lowercase() }) {
                                    Toast.makeText(context, "This source already exists!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                coroutineScope.launch {
                                    viewModel.addPaymentSource(
                                        PaymentSource(
                                            name = nameClean,
                                            color = selectedColorHex,
                                            smartKeywords = newSmartKeywords.trim().lowercase(),
                                            isCustom = true
                                        )
                                    )
                                    newSourceName = ""
                                    newSmartKeywords = ""
                                    Toast.makeText(context, "Source Added!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentYellow, contentColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_source_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = "Add source")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Source", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section header
            item(key = "sources_header") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ACTIVE SOURCES — $currentMonthLabel",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedText,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Source list with budget inputs
            items(paymentSources, key = { it.name }) { source ->
                val budgetStatus = budgetSummary?.sourceBudgets?.find { it.sourceName == source.name }
                SourceWithBudgetCard(
                    source = source,
                    budgetStatus = budgetStatus,
                    currentYearMonth = currentYearMonth,
                    onSetBudget = { amount, carryOver ->
                        viewModel.setBudgetForMonth(source.name, currentYearMonth, amount, carryOver)
                        Toast.makeText(context, "Budget updated for ${source.name}", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = {
                        coroutineScope.launch {
                            viewModel.deletePaymentSource(source)
                            Toast.makeText(context, "${source.name} removed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SourceWithBudgetCard(
    source: PaymentSource,
    budgetStatus: SourceBudgetStatus?,
    currentYearMonth: String,
    onSetBudget: (amount: Double, carryOver: Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val sourceColor = try {
        Color(android.graphics.Color.parseColor(source.color))
    } catch (_: Exception) { AccentYellow }

    var budgetText by remember(budgetStatus) {
        mutableStateOf(
            if ((budgetStatus?.budgetAmount ?: 0.0) > 0.0)
                String.format(Locale.getDefault(), "%.0f", budgetStatus?.budgetAmount ?: 0.0)
            else ""
        )
    }
    var carryOver by remember(budgetStatus) {
        mutableStateOf(budgetStatus?.carryOver ?: false)
    }
    var isEditing by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Source header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(sourceColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = source.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                    if (!source.isCustom) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(MutedText.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("PRESET", fontSize = 8.sp, color = MutedText, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (source.isCustom) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_source_${source.name}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                    }
                }
            }

            // Keywords
            if (source.smartKeywords.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Keywords: ${source.smartKeywords}",
                    fontSize = 11.sp,
                    color = MutedText.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 26.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CardBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Budget row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Monthly Budget",
                    fontSize = 13.sp,
                    color = MutedText,
                    fontWeight = FontWeight.SemiBold
                )

                if (!isEditing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { isEditing = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (budgetText.isNotEmpty()) "₹$budgetText" else "₹0",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (budgetText.isNotEmpty()) AccentYellow else MutedText.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AccentYellow, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Edit mode
            if (isEditing) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = it },
                        label = { Text("Amount") },
                        prefix = { Text("₹", color = MutedText) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentYellow,
                            focusedLabelColor = AccentYellow,
                            cursorColor = AccentYellow,
                            unfocusedBorderColor = CardBorder,
                            unfocusedTextColor = LightText,
                            focusedTextColor = LightText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("budget_amount_input_${source.name}"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            val amount = budgetText.toDoubleOrNull() ?: 0.0
                            onSetBudget(amount, carryOver)
                            isEditing = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentYellow, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("source_budget_save_${source.name}")
                    ) {
                        Text("Set", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

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
                    checked = carryOver,
                    onCheckedChange = { newValue ->
                        carryOver = newValue
                        val amount = budgetText.toDoubleOrNull() ?: 0.0
                        onSetBudget(amount, newValue)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentYellow,
                        uncheckedThumbColor = MutedText,
                        uncheckedTrackColor = CardBorder
                    ),
                    modifier = Modifier.testTag("budget_carryover_toggle_${source.name}")
                )
            }
        }
    }
}

