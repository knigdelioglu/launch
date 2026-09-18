package io.github.knigdelioglu.seyir.ui

import io.github.knigdelioglu.seyir.data.ThemeMode

internal const val MINUTES_PER_DAY = 24 * 60

internal fun resolveThemeMode(
    manualThemeMode: ThemeMode,
    scheduleEnabled: Boolean,
    startMinutes: Int,
    endMinutes: Int,
    nowMinutes: Int,
): ThemeMode {
    if (!scheduleEnabled) return manualThemeMode
    return if (
        isMinuteInDarkWindow(
            nowMinutes = nowMinutes,
            startMinutes = startMinutes,
            endMinutes = endMinutes,
        )
    ) {
        ThemeMode.BLACK
    } else {
        ThemeMode.DARK
    }
}

internal fun isMinuteInDarkWindow(
    nowMinutes: Int,
    startMinutes: Int,
    endMinutes: Int,
): Boolean {
    val now = normalizeMinuteOfDay(nowMinutes)
    val start = normalizeMinuteOfDay(startMinutes)
    val end = normalizeMinuteOfDay(endMinutes)

    if (start == end) return true
    return if (start < end) {
        now in start until end
    } else {
        now >= start || now < end
    }
}

internal fun formatMinuteOfDay(value: Int): String {
    val normalized = normalizeMinuteOfDay(value)
    val hour = normalized / 60
    val minute = normalized % 60
    return "%02d:%02d".format(hour, minute)
}

internal fun parseMinuteOfDay(value: String): Int? {
    val match = TIME_PATTERN.matchEntire(value.trim()) ?: return null
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun normalizeMinuteOfDay(value: Int): Int =
    ((value % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY

private val TIME_PATTERN = Regex("""^(\d{1,2}):(\d{2})$""")
