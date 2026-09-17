package io.github.knigdelioglu.seyir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.knigdelioglu.seyir.ui.theme.SeyirColors
import io.github.knigdelioglu.seyir.ui.theme.SeyirRadius
import io.github.knigdelioglu.seyir.ui.theme.SeyirSpacing
import io.github.knigdelioglu.seyir.ui.theme.SeyirType

@Composable
internal fun TvTransientMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(bottom = SeyirSpacing.ScreenVertical)
            .background(
                color = SeyirColors.SurfaceElevated,
                shape = RoundedCornerShape(SeyirRadius.Action),
            )
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        Text(
            text = message,
            fontSize = SeyirType.Meta,
            color = SeyirColors.TextPrimary,
        )
    }
}
