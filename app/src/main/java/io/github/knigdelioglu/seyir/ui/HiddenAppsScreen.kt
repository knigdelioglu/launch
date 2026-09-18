package io.github.knigdelioglu.seyir.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.knigdelioglu.seyir.data.InstalledApp
import io.github.knigdelioglu.seyir.ui.theme.SeyirColors
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
    val appFocusRequesters = remember(apps.map { it.packageName }) {
        apps.associate { it.packageName to FocusRequester() }
    }
    val backFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    var focusTarget by remember { mutableStateOf<String?>(null) }
    var restoreRequest by remember { mutableStateOf(FocusRestoreRequest()) }

    FocusRestoreEffect(
        itemKeys = apps.map { it.packageName },
        focusTarget = focusTarget,
        request = restoreRequest,
    ) { latestFocusTarget ->
        if (apps.isEmpty()) {
            requestFocusBestEffort { backFocusRequester.requestFocus() }
        } else {
            restoreItemFocus(
                itemKeys = apps.map { it.packageName },
                focusTarget = latestFocusTarget,
                isItemVisible = { targetIndex ->
                    gridState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
                },
                scrollToItem = gridState::scrollToItem,
                requestFocus = { key -> appFocusRequesters[key]?.requestFocus() },
            )
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
            TvHeaderAction(
                text = "‹  Tüm Uygulamalar",
                onClick = onBack,
                modifier = Modifier.focusRequester(backFocusRequester),
            )
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
                    state = gridState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SeyirSpacing.Item),
                    verticalArrangement = Arrangement.spacedBy(SeyirSpacing.Section),
                ) {
                    itemsIndexed(
                        items = apps,
                        key = { _, app -> app.packageName },
                    ) { _, app ->
                        HiddenAppCard(
                            app = app,
                            onFocused = { focusTarget = app.packageName },
                            onRestore = {
                                val fallback = adjacentFallbackKey(
                                    itemKeys = apps.map { it.packageName },
                                    removedKey = app.packageName,
                                )
                                focusTarget = fallback
                                restoreRequest = restoreRequest.next(FocusRestoreReason.ITEM_REMOVED)
                                onRestore(app)
                            },
                            modifier = Modifier.focusRequester(
                                appFocusRequesters.getValue(app.packageName),
                            ),
                        )
                    }
                }
            }
        }

        transientMessage?.let { message ->
            TvTransientMessage(
                message = message,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun HiddenAppCard(
    app: InstalledApp,
    onFocused: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .width(SeyirSize.AppCardWidth)
            .tvFocusScale(focused, "hidden-app-card-scale")
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .tvDpadClick(onClick = onRestore)
            .focusable()
            .clickable(onClick = onRestore),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = SeyirSize.AppCardWidth,
                    height = SeyirSize.AppCardHeight,
                )
                .background(
                    color = if (focused) SeyirColors.SurfaceFocused else SeyirColors.SurfaceSoft,
                    shape = RoundedCornerShape(SeyirRadius.Card),
                )
                .tvFocusedChasingBorder(focused = focused),
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
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = SeyirType.CardLabel,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            color = if (focused) SeyirColors.TextPrimary else SeyirColors.TextSecondary,
        )
    }
}
