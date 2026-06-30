package com.example.ui.graph

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.ExportConfig
import com.example.data.export.ExportDateRange
import com.example.data.export.ExportResult
import com.example.ui.home.HomeViewModel
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LightText
import com.example.ui.theme.MutedText
import java.text.SimpleDateFormat
import java.util.*

internal enum class PeriodPreset(val label: String) {
    ThisMonth("This Month"),
    Last7Days("7 Days"),
    Last30Days("30 Days"),
    Last90Days("90 Days"),
    Custom("Custom")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportOptionsSheet(
    exportState: HomeViewModel.ExportState,
    onExport: (ExportConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPreset by remember { mutableStateOf(PeriodPreset.ThisMonth) }
    var privacyMode by remember { mutableStateOf(false) }
    var customFromMs by remember { mutableStateOf<Long?>(null) }
    var customToMs by remember { mutableStateOf<Long?>(null) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val isLoading = exportState is HomeViewModel.ExportState.Loading
    val isExportEnabled = !isLoading &&
            (selectedPreset != PeriodPreset.Custom || (customFromMs != null && customToMs != null))

    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    // ── Date range picker dialog ─────────────────────────────────────────────
    if (showDateRangePicker) {
        val rangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = customFromMs,
            initialSelectedEndDateMillis = customToMs
        )
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    customFromMs = rangePickerState.selectedStartDateMillis
                    // +86 399 999 ms to include the full end day (23:59:59.999)
                    customToMs = rangePickerState.selectedEndDateMillis?.plus(86_399_999L)
                    showDateRangePicker = false
                }) {
                    Text("Select", color = AccentYellow, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Cancel", color = MutedText)
                }
            }
        ) {
            DateRangePicker(
                state = rangePickerState,
                modifier = Modifier.weight(1f)
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Export Report",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
                Box(
                    modifier = Modifier
                        .background(AccentYellow.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "CSV",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentYellow
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Period label ──────────────────────────────────────────────────
            Text(
                "PERIOD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(PeriodPreset.entries.toList()) { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = { selectedPreset = preset },
                        label = {
                            Text(
                                preset.label,
                                fontSize = 13.sp,
                                fontWeight = if (selectedPreset == preset) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentYellow,
                            selectedLabelColor = Color.White,
                            containerColor = DarkBg,
                            labelColor = MutedText
                        )
                    )
                }
            }

            // ── Custom date range ─────────────────────────────────────────────
            AnimatedVisibility(visible = selectedPreset == PeriodPreset.Custom) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = if (customFromMs != null) AccentYellow.copy(alpha = 0.5f) else CardBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { showDateRangePicker = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Date Range",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedText
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (customFromMs != null && customToMs != null)
                                    "${dateFmt.format(Date(customFromMs!!))} – ${dateFmt.format(Date(customToMs!!))}"
                                else
                                    "Tap to select",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (customFromMs != null) LightText else MutedText.copy(alpha = 0.5f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick date range",
                            tint = if (customFromMs != null) AccentYellow else MutedText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = CardBorder)
            Spacer(modifier = Modifier.height(20.dp))

            // ── Privacy toggle ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (privacyMode) AccentYellow.copy(alpha = 0.12f) else DarkBg,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (privacyMode) AccentYellow else MutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Privacy Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                        Text(
                            "Mask SMS sender & raw message content",
                            fontSize = 11.sp,
                            color = MutedText
                        )
                    }
                }
                Switch(
                    checked = privacyMode,
                    onCheckedChange = { privacyMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentYellow,
                        uncheckedThumbColor = MutedText,
                        uncheckedTrackColor = DarkBg
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Export button ─────────────────────────────────────────────────
            Button(
                onClick = {
                    val range: ExportDateRange = when (selectedPreset) {
                        PeriodPreset.ThisMonth  -> ExportDateRange.CurrentMonth
                        PeriodPreset.Last7Days  -> ExportDateRange.Last7Days
                        PeriodPreset.Last30Days -> ExportDateRange.Last30Days
                        PeriodPreset.Last90Days -> ExportDateRange.Last90Days
                        PeriodPreset.Custom     -> ExportDateRange.Custom(customFromMs!!, customToMs!!)
                    }
                    onExport(ExportConfig(dateRange = range, privacyMode = privacyMode))
                },
                enabled = isExportEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentYellow,
                    contentColor = Color.White,
                    disabledContainerColor = AccentYellow.copy(alpha = 0.35f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Generating report…",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedPreset == PeriodPreset.Custom && customFromMs == null)
                            "Select date range first"
                        else
                            "Export CSV",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/** Write CSV to a user-selected document Uri from system file picker. */
fun saveExportCsvToUri(context: Context, uri: Uri, result: ExportResult): Boolean {
    try {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(result.csvContent.toByteArray(Charsets.UTF_8))
            output.flush()
        } ?: return false
        return true
    } catch (e: Exception) {
        Log.e("ExportSheet", "Failed to save CSV: ${e.message}", e)
        return false
    }
}

