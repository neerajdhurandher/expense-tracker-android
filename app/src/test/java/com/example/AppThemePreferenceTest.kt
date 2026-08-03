package com.example

import com.example.data.model.AppThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemePreferenceTest {

    @Test
    fun fromStorage_defaultsToLightForMissingOrUnknownValue() {
        assertEquals(AppThemePreference.LIGHT, AppThemePreference.fromStorage(null))
        assertEquals(AppThemePreference.LIGHT, AppThemePreference.fromStorage("unknown"))
    }

    @Test
    fun fromStorage_parsesKnownValuesIgnoringCase() {
        assertEquals(AppThemePreference.LIGHT, AppThemePreference.fromStorage("LIGHT"))
        assertEquals(AppThemePreference.DARK, AppThemePreference.fromStorage("dark"))
        assertEquals(AppThemePreference.SYSTEM, AppThemePreference.fromStorage("System"))
    }

    @Test
    fun resolvesToDarkTheme_respectsSystemOnlyForSystemMode() {
        assertFalse(AppThemePreference.LIGHT.resolvesToDarkTheme(isSystemDark = true))
        assertTrue(AppThemePreference.DARK.resolvesToDarkTheme(isSystemDark = false))
        assertTrue(AppThemePreference.SYSTEM.resolvesToDarkTheme(isSystemDark = true))
        assertFalse(AppThemePreference.SYSTEM.resolvesToDarkTheme(isSystemDark = false))
    }
}

