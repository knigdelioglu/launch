package io.github.knigdelioglu.seyir.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val SeyirColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = Color(0xFF09090C),
    onBackground = Color.White,
    surface = Color(0xFF111116),
    onSurface = Color.White,
)

@Composable
fun SeyirTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SeyirColors,
        content = content,
    )
}
