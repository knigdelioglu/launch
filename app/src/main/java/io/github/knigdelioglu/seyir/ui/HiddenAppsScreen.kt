package io.github.knigdelioglu.seyir.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.knigdelioglu.seyir.data.InstalledApp
import io.github.knigdelioglu.seyir.ui.theme.SeyirColors
import io.github.knigdelioglu.seyir.ui.theme.SeyirMotion
import io.github.knigdelioglu.seyir.ui.theme.SeyirRadius
import io.github.knigdelioglu.seyir.ui.theme.SeyirSize
import io.github.knigdelioglu.seyir.ui.theme.SeyirSpacing
import io.github.knigdelioglu.seyir.ui.theme.SeyirType
import kotlinx.coroutines.delay

@Composable
fun HiddenAppsScreen(
    apps: List<InstalledApp>,
    transientMessage: String?,
    onRestore: (InstalledApp) -> Unit,
    onBack: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    val firstFocusRequester = remember { FocusRequester() }

    LaunchedEffect(apps) {
        if (apps.isNotEmpty()) {
            delay(120)
            runCatching { firstFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(transientMessage) {
        if (transientMessage != null) {
            delay(2_500)
            onDismissMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        SeyirColors.BackgroundTop,
                        SeyirColors.BackgroundMiddle,
                        SeyirColors.BackgroundBottom,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = SeyirSpacing.ScreenHorizontal,
                    vertical = SeyirSpacing.ScreenVertical,
                ),
        ) {
            BackAction(onClick = onBack)
            Spacer(modifier = Modifier.height(SeyirSpacing.Section))

            Text(
                text = "Gizlenen Uygulamalar",
                fontSize = SeyirType.Hero,
                fontWeight = FontWeight.SemiBold,
                color = SeyirColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
            Text(
                text = if (apps.isEmpty()) {
                    "Gizlenmiş uygulama yok."
                } else {
                    "${apps.size} uygulama  •  OK: yeniden göster"
                },
                fontSize = SeyirType.CardLabel,
                color = SeyirColors.TextTertiary,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Section))

            if (apps.isEmpty()) {
                Text(
                    text = "Gizlediğiniz uygulamalar burada görünür.",
                    color = SeyirColors.TextSecondary,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SeyirSpacing.Item),
                    verticalArrangement = Arrangement.spacedBy(SeyirSpacing.Section),
                ) {
                    itemsIndexed(
                        items = apps,
                        key = { _, app -> app.packageName },
                    ) { index, app ->
                        HiddenAppCard(
                            app = app,
                            onRestore = { onRestore(app) },
                            modifier = if (index == 0) {
                                Modifier.focusRequester(firstFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
        }

        transientMessage?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = SeyirSpacing.ScreenVertical)
                    .clip(RoundedCornerShape(SeyirRadius.Action))
                    .background(SeyirColors.SurfaceElevated)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(
                    text = message,
                    fontSize = SeyirType.Meta,
                    color = SeyirColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun BackAction(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }

    Text(
        text = "‹  Tüm Uygulamalar",
        modifier = Modifier
            .clip(RoundedCornerShape(SeyirRadius.Pill))
            .background(
                if (focused) SeyirColors.SurfaceFocused else SeyirColors.SurfaceSoft,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        fontSize = SeyirType.Meta,
        fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
        color = if (focused) SeyirColors.TextPrimary else SeyirColors.TextSecondary,
    )
}

@Composable
private fun HiddenAppCard(
    app: InstalledApp,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) SeyirMotion.FocusScale else 1f,
        animationSpec = tween(SeyirMotion.FocusDurationMs),
        label = "hidden-app-card-scale",
    )

    Column(
        modifier = modifier
            .width(SeyirSize.AppCardWidth)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onRestore),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = SeyirSize.AppCardWidth,
                    height = SeyirSize.AppCardHeight,
                )
                .clip(RoundedCornerShape(SeyirRadius.Card))
                .background(
                    if (focused) SeyirColors.SurfaceFocused else SeyirColors.SurfaceSoft,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(SeyirSize.AppIcon),
            )
        }
        Spacer(modifier = Modifier.height(SeyirSpacing.Compact))
        Text(
            text = app.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = SeyirType.CardLabel,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            color = if (focused) SeyirColors.TextPrimary else SeyirColors.TextSecondary,
        )
    }
}
