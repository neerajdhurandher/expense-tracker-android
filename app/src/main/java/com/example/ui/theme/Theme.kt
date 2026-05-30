package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ExpenseTrackerColorScheme = lightColorScheme(
    primary = AccentYellow,
    onPrimary = OnAccent,
    primaryContainer = DarkSurface,
    onPrimaryContainer = LightText,
    secondary = AccentYellow,
    onSecondary = OnAccent,
    background = DarkBg,
    onBackground = LightText,
    surface = DarkSurface,
    onSurface = LightText,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = MutedText,
    error = ErrorRed,
    onError = OnAccent
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Use light professional theme now
    dynamicColor: Boolean = false, // Disable dynamic colors to stick to design tokens
    content: @Composable () -> Unit
) {
    val colorScheme = ExpenseTrackerColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            // Light status bars = true, since we are in deep light theme now
            windowInsetsController.isAppearanceLightStatusBars = true
            windowInsetsController.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
