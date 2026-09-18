package io.github.knigdelioglu.seyir.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import io.github.knigdelioglu.seyir.data.AccentMode
import io.github.knigdelioglu.seyir.data.ThemeMode

@Composable
fun SeyirTheme(
    themeMode: ThemeMode,
    accentMode: AccentMode,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val palette = remember(themeMode, accentMode) {
        paletteFor(themeMode, accentMode)
    }
    val motion = remember(reducedMotion) {
        if (reducedMotion) {
            SeyirMotionTokens(
                focusScale = 1f,
                focusDurationMs = 0,
                focusEasing = FastOutSlowInEasing,
            )
        } else {
            DefaultSeyirMotion
        }
    }
    val colorScheme = remember(palette) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = Color.Black,
            background = palette.backgroundMiddle,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
        )
    }

    CompositionLocalProvider(
        LocalSeyirPalette provides palette,
        LocalSeyirMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

private fun paletteFor(
    themeMode: ThemeMode,
    accentMode: AccentMode,
): SeyirPalette {
    val accent = when (accentMode) {
        AccentMode.NEUTRAL -> if (themeMode == ThemeMode.BLACK) Color(0xFFA8C7FA) else Color(0xFF0F172A)
        AccentMode.BLUE -> if (themeMode == ThemeMode.BLACK) Color(0xFFA8C7FA) else Color(0xFF1D4ED8)
        AccentMode.EMERALD -> if (themeMode == ThemeMode.BLACK) Color(0xFF6DD58C) else Color(0xFF047857)
    }

    val base = when (themeMode) {
        ThemeMode.DARK -> DefaultSeyirPalette
        ThemeMode.BLACK -> DefaultSeyirPalette.copy(
            // Google TV / Material-style dark gray rather than OLED black.
            backgroundTop = Color(0xFF17181B),
            backgroundMiddle = Color(0xFF131416),
            backgroundBottom = Color(0xFF101113),
            surface = Color(0xFF1E1F22),
            surfaceElevated = Color(0xFF28292D),
            surfaceSoft = Color.White.copy(alpha = 0.075f),
            hairline = Color.White.copy(alpha = 0.12f),
            textPrimary = Color(0xFFE3E3E3),
            textSecondary = Color(0xFFC4C7C5),
            textTertiary = Color(0xFF8E918F),
        )
    }

    return base.copy(
        accent = accent,
        surfaceFocused = if (themeMode == ThemeMode.BLACK) {
            accent.copy(alpha = 0.20f)
        } else {
            Color.White.copy(alpha = 0.35f)
        },
    )
}
