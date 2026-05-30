package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LightText
import com.example.ui.theme.MutedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormSheet(
    initialName: String = "",
    initialAmount: Double? = null,
    initialCategory: String = "Other",
    isEditMode: Boolean = false,
    categories: List<Category> = emptyList(),
    onSave: (name: String, amount: Double, category: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var amountStr by remember { mutableStateOf(initialAmount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var expandedDropdown by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        // Form Title
        Text(
            text = when {
                isEditMode -> "Edit Expense"
                initialAmount != null -> "Capture Expense"
                else -> "Log Expense Manually"
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = LightText,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Title/Name Field
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                if (it.trim().isNotEmpty()) nameError = null
            },
            label = { Text("Expense Title / Merchant") },
            placeholder = { Text("e.g. Swiggy, Uber, Rent") },
            leadingIcon = { Icon(Icons.Default.Label, contentDescription = "Title icon") },
            isError = nameError != null,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentYellow,
                focusedLabelColor = AccentYellow,
                cursorColor = AccentYellow,
                unfocusedBorderColor = MutedText.copy(alpha = 0.5f),
                unfocusedTextColor = LightText,
                focusedTextColor = LightText
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("expense_name_input"),
            shape = RoundedCornerShape(12.dp)
        )
        if (nameError != null) {
            Text(
                text = nameError!!,
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
            )
        }

        // Amount Field
        OutlinedTextField(
            value = amountStr,
            onValueChange = {
                amountStr = it
                if (it.toDoubleOrNull() != null) amountError = null
            },
            label = { Text("Amount (₹)") },
            placeholder = { Text("0.00") },
            leadingIcon = { Icon(Icons.Default.Paid, contentDescription = "Amount currency") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = amountError != null,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentYellow,
                focusedLabelColor = AccentYellow,
                cursorColor = AccentYellow,
                unfocusedBorderColor = MutedText.copy(alpha = 0.5f),
                unfocusedTextColor = LightText,
                focusedTextColor = LightText
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("expense_amount_input"),
            shape = RoundedCornerShape(12.dp)
        )
        if (amountError != null) {
            Text(
                text = amountError!!,
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
            )
        }

        // Category Selection Custom Flow with Dropdown Menu
        Text(
            text = "Category",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MutedText,
            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            val matchingColHex = categories.find { it.name == selectedCategory }?.color ?: "#ADB5BD"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, MutedText.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expandedDropdown = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category small visual bullet dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(android.graphics.Color.parseColor(matchingColHex)), RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selectedCategory,
                        color = LightText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown icon",
                    tint = MutedText
                )
            }

            DropdownMenu(
                expanded = expandedDropdown,
                onDismissRequest = { expandedDropdown = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(DarkSurface)
                    .border(1.dp, MutedText.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(android.graphics.Color.parseColor(category.color)), RoundedCornerShape(5.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = category.name, color = LightText, fontSize = 15.sp)
                            }
                        },
                        onClick = {
                            selectedCategory = category.name
                            expandedDropdown = false
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

        // Context check disclaimer if it is from parsed sms
        if (initialAmount != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AccentYellow.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .border(1.dp, AccentYellow.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Auto parsed information",
                    tint = AccentYellow,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "This expense was auto-parsed from your message inbox.",
                    fontSize = 12.sp,
                    color = LightText.copy(alpha = 0.9f)
                )
            }
        }

        // Action Buttons Row (Dismiss & Save)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(24.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LightText),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("expense_dismiss_btn")
            ) {
                Text(
                    text = "Dismiss",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Button(
                onClick = {
                    val finalName = name.trim()
                    val amount = amountStr.toDoubleOrNull()

                    if (finalName.isEmpty()) {
                        nameError = "Merchant name is required."
                    }
                    if (amount == null || amount <= 0.0) {
                        amountError = "Provide a valid transaction amount."
                    }

                    if (finalName.isNotEmpty() && amount != null && amount > 0.0) {
                        onSave(finalName, amount, selectedCategory)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentYellow,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("expense_save_btn")
            ) {
                Text(
                    text = if (isEditMode) "Update" else "Save",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
