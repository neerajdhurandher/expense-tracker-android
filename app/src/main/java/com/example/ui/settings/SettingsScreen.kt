package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.auth.AuthViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToSourceBudget: () -> Unit,
    onSignOut: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current

    var isEditingProfile by remember { mutableStateOf(false) }
    var editName by remember(currentUser) { mutableStateOf(currentUser?.displayName ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Profile Card ──
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    .testTag("profile_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(AccentYellow.copy(alpha = 0.12f), CircleShape)
                                    .border(2.dp, AccentYellow, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (currentUser?.displayName ?: "U").take(1).uppercase(),
                                    color = AccentYellow,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Name & email
                            Column {
                                Text(
                                    text = currentUser?.displayName ?: "User",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentUser?.email ?: "",
                                    fontSize = 13.sp,
                                    color = MutedText
                                )
                            }
                        }

                        // Edit toggle button
                        if (!isEditingProfile) {
                            IconButton(
                                onClick = {
                                    editName = currentUser?.displayName ?: ""
                                    isEditingProfile = true
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AccentYellow.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                                    .testTag("profile_edit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit profile",
                                    tint = AccentYellow,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Edit form — expands below profile info
                    if (isEditingProfile) {
                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = CardBorder)

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Display Name") },
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
                                .testTag("profile_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isEditingProfile = false },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedText),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("profile_cancel_button")
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Button(
                                onClick = {
                                    val trimmed = editName.trim()
                                    if (trimmed.isEmpty()) {
                                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    authViewModel.updateProfile(trimmed) { result ->
                                        if (result.isSuccess) {
                                            isEditingProfile = false
                                            Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentYellow,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("profile_save_button")
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Section: General ──
            Text(
                text = "GENERAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            SettingsRow(
                icon = Icons.Default.Category,
                title = "Categories",
                subtitle = "Manage expense categories",
                onClick = onNavigateToCategories,
                testTag = "settings_categories_row"
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsRow(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Sources & Budget",
                subtitle = "Manage payment sources and monthly budgets",
                onClick = onNavigateToSourceBudget,
                testTag = "settings_source_budget_row"
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Section: Account ──
            Text(
                text = "ACCOUNT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            // Sign out row
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSignOut() }
                    .testTag("settings_sign_out")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ErrorRed.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign out",
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Sign Out",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ErrorRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Footer ──
            Text(
                text = "Expense Tracker v1.0",
                fontSize = 12.sp,
                color = MutedText.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Reusable settings row component — extensible for future settings items.
 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AccentYellow.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = AccentYellow,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LightText
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MutedText.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

