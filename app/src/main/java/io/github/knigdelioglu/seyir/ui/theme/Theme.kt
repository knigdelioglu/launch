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
        AccentMode.NEUTRAL -> if (themeMode == ThemeMode.BLACK) Color.White else Color(0xFF0F172A)
        AccentMode.BLUE -> if (themeMode == ThemeMode.BLACK) Color(0xFF78B7FF) else Color(0xFF1D4ED8)
        AccentMode.EMERALD -> if (themeMode == ThemeMode.BLACK) Color(0xFF78D5AE) else Color(0xFF047857)
    }

    val base = when (themeMode) {
        ThemeMode.DARK -> DefaultSeyirPalette
        ThemeMode.BLACK -> DefaultSeyirPalette.copy(
            backgroundTop = Color.Black,
            backgroundMiddle = Color.Black,
            backgroundBottom = Color.Black,
            surface = Color(0xFF0A0A0A),
            surfaceElevated = Color(0xFF111111),
            surfaceSoft = Color.White.copy(alpha = 0.045f),
            hairline = Color.White.copy(alpha = 0.08f),
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.68f),
            textTertiary = Color.White.copy(alpha = 0.44f),
        )
    }

    return base.copy(
        accent = accent,
        surfaceFocused = if (themeMode == ThemeMode.BLACK) {
            (if (accentMode == AccentMode.NEUTRAL) Color.White else accent).copy(
                alpha = if (accentMode == AccentMode.NEUTRAL) 0.145f else 0.19f,
            )
        } else {
            Color.White.copy(alpha = 0.35f)
        },
    )
}
