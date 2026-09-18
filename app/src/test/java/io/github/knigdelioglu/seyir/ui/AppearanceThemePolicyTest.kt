package io.github.knigdelioglu.seyir.ui

import io.github.knigdelioglu.seyir.data.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceThemePolicyTest {
    @Test
    fun overnightSchedule_isDarkAcrossMidnight() {
        assertTrue(isMinuteInDarkWindow(23 * 60, 20 * 60, 7 * 60))
        assertTrue(isMinuteInDarkWindow(6 * 60 + 59, 20 * 60, 7 * 60))
        assertFalse(isMinuteInDarkWindow(12 * 60, 20 * 60, 7 * 60))
    }

    @Test
    fun scheduledTheme_overridesManualThemeOnlyInsideWindow() {
        assertEquals(
            ThemeMode.BLACK,
            resolveThemeMode(
                manualThemeMode = ThemeMode.DARK,
                scheduleEnabled = true,
                startMinutes = 20 * 60,
                endMinutes = 7 * 60,
                nowMinutes = 22 * 60,
            ),
        )
        assertEquals(
            ThemeMode.DARK,
            resolveThemeMode(
                manualThemeMode = ThemeMode.BLACK,
                scheduleEnabled = true,
                startMinutes = 20 * 60,
                endMinutes = 7 * 60,
                nowMinutes = 12 * 60,
            ),
        )
    }

    @Test
    fun disabledSchedule_preservesManualChoice() {
        assertEquals(
            ThemeMode.BLACK,
            resolveThemeMode(
                manualThemeMode = ThemeMode.BLACK,
                scheduleEnabled = false,
                startMinutes = 20 * 60,
                endMinutes = 7 * 60,
                nowMinutes = 12 * 60,
            ),
        )
    }

    @Test
    fun timeText_roundTripsAndRejectsInvalidValues() {
        assertEquals(20 * 60 + 30, parseMinuteOfDay("20:30"))
        assertEquals("07:05", formatMinuteOfDay(7 * 60 + 5))
        assertNull(parseMinuteOfDay("24:00"))
        assertNull(parseMinuteOfDay("7:5"))
    }
}
