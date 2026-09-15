package io.github.knigdelioglu.seyir.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val SeyirColorScheme = darkColorScheme(
    primary = SeyirColors.TextPrimary,
    onPrimary = Color.Black,
    background = SeyirColors.BackgroundMiddle,
    onBackground = SeyirColors.TextPrimary,
    surface = SeyirColors.Surface,
    onSurface = SeyirColors.TextPrimary,
    surfaceVariant = SeyirColors.SurfaceElevated,
    onSurfaceVariant = SeyirColors.TextSecondary,
)

@Composable
fun SeyirTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SeyirColorScheme,
        content = content,
    )
}
