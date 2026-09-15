package io.github.knigdelioglu.seyir.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SeyirColors {
    val BackgroundTop = Color(0xFF111116)
    val BackgroundMiddle = Color(0xFF09090C)
    val BackgroundBottom = Color(0xFF050507)
    val Surface = Color(0xFF15151B)
    val SurfaceElevated = Color(0xFF1B1B22)
    val SurfaceSoft = Color.White.copy(alpha = 0.055f)
    val SurfaceFocused = Color.White.copy(alpha = 0.145f)
    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.68f)
    val TextTertiary = Color.White.copy(alpha = 0.44f)
    val Hairline = Color.White.copy(alpha = 0.09f)
}

object SeyirSpacing {
    val ScreenHorizontal = 64.dp
    val ScreenVertical = 40.dp
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
    const val FocusScale = 1.06f
    const val FocusDurationMs = 160
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
