package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExpenseTrackerColors(
	val background: Color,
	val surface: Color,
	val accent: Color,
	val onAccent: Color,
	val textPrimary: Color,
	val textSecondary: Color,
	val error: Color,
	val border: Color
)

internal val LightExpenseTrackerColors = ExpenseTrackerColors(
	background = Color(0xFFF3F4F9),
	surface = Color(0xFFFFFFFF),
	accent = Color(0xFF4F46E5),
	onAccent = Color(0xFFFFFFFF),
	textPrimary = Color(0xFF0F172A),
	textSecondary = Color(0xFF475569),
	error = Color(0xFFEF4444),
	border = Color(0xFFE2E8F0)
)

internal val DarkExpenseTrackerColors = ExpenseTrackerColors(
	background = Color(0xFF020617),
	surface = Color(0xFF111827),
	accent = Color(0xFF5B6BEA),
	onAccent = Color(0xFF020617),
	textPrimary = Color(0xFFF8FAFC),
	textSecondary = Color(0xFF94A3B8),
	error = Color(0xFFF87171),
	border = Color(0xFF1E293B)
)

internal val LocalExpenseTrackerColors = staticCompositionLocalOf { LightExpenseTrackerColors }

val DarkBg: Color
	@Composable get() = LocalExpenseTrackerColors.current.background

val DarkSurface: Color
	@Composable get() = LocalExpenseTrackerColors.current.surface

val AccentYellow: Color
	@Composable get() = LocalExpenseTrackerColors.current.accent

val OnAccent: Color
	@Composable get() = LocalExpenseTrackerColors.current.onAccent

val LightText: Color
	@Composable get() = LocalExpenseTrackerColors.current.textPrimary

val MutedText: Color
	@Composable get() = LocalExpenseTrackerColors.current.textSecondary

val ErrorRed: Color
	@Composable get() = LocalExpenseTrackerColors.current.error

val CardBorder: Color
	@Composable get() = LocalExpenseTrackerColors.current.border
