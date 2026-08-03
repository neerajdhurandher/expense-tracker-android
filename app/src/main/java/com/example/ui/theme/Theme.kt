package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.model.AppThemePreference

private fun lightExpenseTrackerColorScheme() = lightColorScheme(
    primary = LightExpenseTrackerColors.accent,
    onPrimary = LightExpenseTrackerColors.onAccent,
    primaryContainer = LightExpenseTrackerColors.surface,
    onPrimaryContainer = LightExpenseTrackerColors.textPrimary,
    secondary = LightExpenseTrackerColors.accent,
    onSecondary = LightExpenseTrackerColors.onAccent,
    background = LightExpenseTrackerColors.background,
    onBackground = LightExpenseTrackerColors.textPrimary,
    surface = LightExpenseTrackerColors.surface,
    onSurface = LightExpenseTrackerColors.textPrimary,
    surfaceVariant = LightExpenseTrackerColors.surface,
    onSurfaceVariant = LightExpenseTrackerColors.textSecondary,
    error = LightExpenseTrackerColors.error,
    onError = LightExpenseTrackerColors.onAccent,
    outline = LightExpenseTrackerColors.border
)

private fun darkExpenseTrackerColorScheme() = darkColorScheme(
    primary = DarkExpenseTrackerColors.accent,
    onPrimary = DarkExpenseTrackerColors.onAccent,
    primaryContainer = DarkExpenseTrackerColors.surface,
    onPrimaryContainer = DarkExpenseTrackerColors.textPrimary,
    secondary = DarkExpenseTrackerColors.accent,
    onSecondary = DarkExpenseTrackerColors.onAccent,
    background = DarkExpenseTrackerColors.background,
    onBackground = DarkExpenseTrackerColors.textPrimary,
    surface = DarkExpenseTrackerColors.surface,
    onSurface = DarkExpenseTrackerColors.textPrimary,
    surfaceVariant = DarkExpenseTrackerColors.surface,
    onSurfaceVariant = DarkExpenseTrackerColors.textSecondary,
    error = DarkExpenseTrackerColors.error,
    onError = DarkExpenseTrackerColors.onAccent,
    outline = DarkExpenseTrackerColors.border
)

@Composable
fun MyApplicationTheme(
    themePreference: AppThemePreference = AppThemePreference.LIGHT,
    dynamicColor: Boolean = false, // Disable dynamic colors to stick to design tokens
    content: @Composable () -> Unit
) {
    val useDarkTheme = themePreference.resolvesToDarkTheme(isSystemInDarkTheme())
    val palette = if (useDarkTheme) DarkExpenseTrackerColors else LightExpenseTrackerColors
    val colorScheme = if (useDarkTheme) darkExpenseTrackerColorScheme() else lightExpenseTrackerColorScheme()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = !useDarkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    CompositionLocalProvider(LocalExpenseTrackerColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
