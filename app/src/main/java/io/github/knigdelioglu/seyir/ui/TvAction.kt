package io.github.knigdelioglu.seyir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.knigdelioglu.seyir.ui.theme.SeyirColors
import io.github.knigdelioglu.seyir.ui.theme.SeyirRadius
import io.github.knigdelioglu.seyir.ui.theme.SeyirType

@Composable
internal fun TvAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
    unfocusedContainerColor: Color = SeyirColors.SurfaceSoft,
    unfocusedFontWeight: FontWeight = FontWeight.Normal,
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SeyirRadius.Action))
            .background(
                when {
                    !enabled -> SeyirColors.SurfaceSoft.copy(alpha = 0.45f)
                    focused -> SeyirColors.SurfaceFocused
                    else -> unfocusedContainerColor
                },
            )
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick(enabled = enabled, onClick = onClick)
            .focusable(enabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
    ) {
        Text(
            text = text,
            fontSize = SeyirType.CardLabel,
            fontWeight = if (focused) FontWeight.SemiBold else unfocusedFontWeight,
            color = when {
                !enabled -> SeyirColors.TextTertiary
                focused -> SeyirColors.TextPrimary
                else -> SeyirColors.TextSecondary
            },
        )
    }
}

@Composable
internal fun TvHeaderAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(SeyirRadius.Pill))
            .background(
                if (focused) SeyirColors.SurfaceFocused else SeyirColors.SurfaceSoft,
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused?.invoke()
            }
            .tvDpadClick(onClick = onClick)
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        fontSize = SeyirType.Meta,
        fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
        color = if (focused) SeyirColors.TextPrimary else SeyirColors.TextSecondary,
    )
}
