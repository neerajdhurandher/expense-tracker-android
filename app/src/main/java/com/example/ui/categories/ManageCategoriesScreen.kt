package com.example.ui.categories

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.PaymentSource
import com.example.ui.home.HomeViewModel
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LightText
import com.example.ui.theme.MutedText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val paymentSources by viewModel.paymentSources.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Categories", "Sources")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage", fontWeight = FontWeight.Bold, color = LightText) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button_categories")) {
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
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = AccentYellow,
                indicator = { @Composable {} },
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) AccentYellow else MutedText
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> CategoryManagementContent(
                    categories = categories,
                    viewModel = viewModel,
                    coroutineScope = coroutineScope,
                    context = context
                )
                1 -> SourceManagementContent(
                    paymentSources = paymentSources,
                    viewModel = viewModel,
                    coroutineScope = coroutineScope,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun CategoryManagementContent(
    categories: List<Category>,
    viewModel: HomeViewModel,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    var newCategoryName by remember { mutableStateOf("") }
    val colorPresets = listOf(
        "#FF6B6B", "#4DABF7", "#51CF66", "#FCC419",
        "#BE4BDB", "#FF922B", "#20C997", "#ADB5BD",
        "#FF8787", "#74C0FC", "#63E6BE", "#FFD43B"
    )
    var selectedColorHex by remember { mutableStateOf(colorPresets.first()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .imePadding()
    ) {
        // Section 1: Create Custom Category Form
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = "ADD CUSTOM CATEGORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentYellow,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Subscriptions, Fitness") },
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
                        .testTag("category_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pick Theme Accent Color",
                    fontSize = 13.sp,
                    color = MutedText,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Sliding presets row
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
                                .testTag("color_chip_$hex"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColorHex == hex) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected color",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val nameClean = newCategoryName.trim()
                        if (nameClean.isEmpty()) {
                            Toast.makeText(context, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (categories.any { it.name.lowercase() == nameClean.lowercase() }) {
                            Toast.makeText(context, "This category already exists!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        coroutineScope.launch {
                            viewModel.addCategory(Category(name = nameClean, color = selectedColorHex, isCustom = true))
                            newCategoryName = ""
                            Toast.makeText(context, "Category Added!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentYellow, contentColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_category_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = "Add category")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Category", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 2: Manage category allocations list
        Text(
            text = "Active Categories",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = LightText,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(categories) { category ->
                val colorHex = Color(android.graphics.Color.parseColor(category.color))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(14.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(colorHex, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = category.name,
                            color = LightText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!category.isCustom) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(MutedText.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRESET",
                                    fontSize = 8.sp,
                                    color = MutedText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Users can only delete custom categories to preserve standard classifier keywords mapping
                    if (category.isCustom) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.deleteCategory(category)
                                    Toast.makeText(context, "${category.name} removed.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("delete_category_${category.name}").size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ErrorRed
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
private fun SourceManagementContent(
    paymentSources: List<PaymentSource>,
    viewModel: HomeViewModel,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    var newSourceName by remember { mutableStateOf("") }
    var newSmartKeywords by remember { mutableStateOf("") }
    val colorPresets = listOf(
        "#FF6B6B", "#4DABF7", "#51CF66", "#FCC419",
        "#BE4BDB", "#FF922B", "#20C997", "#ADB5BD",
        "#FF8787", "#74C0FC", "#63E6BE", "#FFD43B"
    )
    var selectedColorHex by remember { mutableStateOf(colorPresets[4]) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .imePadding()
    ) {
        // Section 1: Add Custom Source Form
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = "ADD CUSTOM SOURCE",
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

                Text(
                    text = "Pick Theme Accent Color",
                    fontSize = 13.sp,
                    color = MutedText,
                    fontWeight = FontWeight.SemiBold
                )

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
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected color",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
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

        Spacer(modifier = Modifier.height(24.dp))

        // Section 2: Active Sources list
        Text(
            text = "Active Sources",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = LightText,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(paymentSources) { source ->
                val sourceColor = Color(android.graphics.Color.parseColor(source.color))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(14.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(sourceColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = source.name,
                                color = LightText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!source.isCustom) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(MutedText.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRESET",
                                        fontSize = 8.sp,
                                        color = MutedText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (source.smartKeywords.isNotBlank()) {
                            Text(
                                text = "smartKeywords: ${source.smartKeywords}",
                                fontSize = 11.sp,
                                color = MutedText.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 32.dp, top = 4.dp)
                            )
                        } else {
                            Text(
                                text = "smartKeywords: (none)",
                                fontSize = 11.sp,
                                color = MutedText.copy(alpha = 0.4f),
                                modifier = Modifier.padding(start = 32.dp, top = 4.dp)
                            )
                        }
                    }

                    // Only custom sources can be deleted
                    if (source.isCustom) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.deletePaymentSource(source)
                                    Toast.makeText(context, "${source.name} removed.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .testTag("delete_source_${source.name}")
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ErrorRed
                            )
                        }
                    }
                }
            }
        }
    }
}
