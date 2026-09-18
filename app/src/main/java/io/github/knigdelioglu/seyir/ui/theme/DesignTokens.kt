package io.github.knigdelioglu.seyir.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SeyirPalette(
    val backgroundTop: Color,
    val backgroundMiddle: Color,
    val backgroundBottom: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceSoft: Color,
    val surfaceFocused: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val hairline: Color,
    val accent: Color,
)

data class SeyirMotionTokens(
    val focusScale: Float,
    val focusDurationMs: Int,
    val focusEasing: Easing,
)

internal val DefaultSeyirPalette = SeyirPalette(
    backgroundTop = Color(0xFFA8C7FA),
    backgroundMiddle = Color(0xFFA8C7FA),
    backgroundBottom = Color(0xFFA8C7FA),
    surface = Color.White.copy(alpha = 0.85f),
    surfaceElevated = Color.White,
    surfaceSoft = Color.White.copy(alpha = 0.20f),
    surfaceFocused = Color.White.copy(alpha = 0.35f),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF334155),
    textTertiary = Color(0xFF64748B),
    hairline = Color(0xFF0F172A).copy(alpha = 0.12f),
    accent = Color(0xFF1D4ED8),
)

internal val DefaultSeyirMotion = SeyirMotionTokens(
    focusScale = 1.05f,
    focusDurationMs = 120,
    focusEasing = FastOutSlowInEasing,
)

internal val LocalSeyirPalette = staticCompositionLocalOf { DefaultSeyirPalette }
internal val LocalSeyirMotion = staticCompositionLocalOf { DefaultSeyirMotion }

object SeyirColors {
    val BackgroundTop: Color
        @Composable get() = LocalSeyirPalette.current.backgroundTop
    val BackgroundMiddle: Color
        @Composable get() = LocalSeyirPalette.current.backgroundMiddle
    val BackgroundBottom: Color
        @Composable get() = LocalSeyirPalette.current.backgroundBottom
    val Surface: Color
        @Composable get() = LocalSeyirPalette.current.surface
    val SurfaceElevated: Color
        @Composable get() = LocalSeyirPalette.current.surfaceElevated
    val SurfaceSoft: Color
        @Composable get() = LocalSeyirPalette.current.surfaceSoft
    val SurfaceFocused: Color
        @Composable get() = LocalSeyirPalette.current.surfaceFocused
    val TextPrimary: Color
        @Composable get() = LocalSeyirPalette.current.textPrimary
    val TextSecondary: Color
        @Composable get() = LocalSeyirPalette.current.textSecondary
    val TextTertiary: Color
        @Composable get() = LocalSeyirPalette.current.textTertiary
    val Hairline: Color
        @Composable get() = LocalSeyirPalette.current.hairline
    val Accent: Color
        @Composable get() = LocalSeyirPalette.current.accent
}

object SeyirSpacing {
    val ScreenHorizontal = 100.dp
    val ScreenVertical = 32.dp
    val SectionLarge = 48.dp
    val Section = 28.dp
    val Item = 18.dp
    val Compact = 12.dp
    val Tiny = 8.dp
}

object SeyirRadius {
    val Card = 20.dp
    val Action = 16.dp
    val Dialog = 24.dp
    val Pill = 999.dp
}

object SeyirSize {
    val AppCardWidth = 176.dp
    val AppCardHeight = 108.dp
    val AppIcon = 62.dp
    val ActionCardWidth = 176.dp
    val ActionCardHeight = 78.dp
}

object SeyirMotion {
    val FocusScale: Float
        @Composable get() = LocalSeyirMotion.current.focusScale
    val FocusDurationMs: Int
        @Composable get() = LocalSeyirMotion.current.focusDurationMs
    val FocusEasing: Easing
        @Composable get() = LocalSeyirMotion.current.focusEasing
}

object SeyirType {
    val Brand = 17.sp
    val Clock = 18.sp
    val Hero = 38.sp
    val Subtitle = 18.sp
    val SectionTitle = 16.sp
    val CardLabel = 15.sp
    val Meta = 13.sp
}
