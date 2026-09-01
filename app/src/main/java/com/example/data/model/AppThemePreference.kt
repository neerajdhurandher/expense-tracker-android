package com.example.data.model

enum class AppThemePreference(val storageValue: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");

    fun resolvesToDarkTheme(isSystemDark: Boolean): Boolean = when (this) {
        LIGHT -> false
        DARK -> true
        SYSTEM -> isSystemDark
    }

    companion object {
        fun fromStorage(value: String?): AppThemePreference {
            return entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: LIGHT
        }
    }
}

