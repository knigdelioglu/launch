package io.github.knigdelioglu.seyir.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import io.github.knigdelioglu.seyir.data.InstalledApp
import io.github.knigdelioglu.seyir.ui.theme.SeyirColors
import io.github.knigdelioglu.seyir.ui.theme.SeyirRadius
import io.github.knigdelioglu.seyir.ui.theme.SeyirSize
import io.github.knigdelioglu.seyir.ui.theme.SeyirSpacing
import io.github.knigdelioglu.seyir.ui.theme.SeyirType
import kotlinx.coroutines.delay

object AllAppsFocusKey {
    const val HIDDEN_APPS = "__hidden_apps__"
}

@Composable
fun AllAppsScreen(
    apps: List<InstalledApp>,
    hiddenAppCount: Int,
    favoritePackageNames: List<String>,
    transientMessage: String?,
    focusTarget: String?,
    onFocusTargetChanged: (String) -> Unit,
    onAppClick: (InstalledApp) -> Unit,
    onToggleFavorite: (InstalledApp) -> Unit,
    onHideApp: (InstalledApp) -> Unit,
    onOpenAppInfo: (InstalledApp) -> Unit,
    onOpenHiddenApps: () -> Unit,
    onBack: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    val favoritePackageSet = remember(favoritePackageNames) { favoritePackageNames.toSet() }
    val appFocusRequesters = remember(apps.map { it.packageName }) {
        apps.associate { it.packageName to FocusRequester() }
    }
    val hiddenAppsFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    var contextApp by remember { mutableStateOf<InstalledApp?>(null) }
    var restoreRequest by remember { mutableStateOf(FocusRestoreRequest()) }

    FocusRestoreEffect(
        itemKeys = apps.map { it.packageName } +
            if (hiddenAppCount > 0) listOf(AllAppsFocusKey.HIDDEN_APPS) else emptyList(),
        focusTarget = focusTarget,
        request = restoreRequest,
    ) { latestFocusTarget ->
        requestAllAppsFocus(
            apps = apps,
            focusTarget = latestFocusTarget,
            appFocusRequesters = appFocusRequesters,
            hiddenAppCount = hiddenAppCount,
            hiddenAppsFocusRequester = hiddenAppsFocusRequester,
            gridState = gridState,
        )
    }

    LaunchedEffect(transientMessage) {
        if (transientMessage != null) {
            delay(2_500)
            onDismissMessage()
        }
    }

    fun closeDialogAndRestore() {
        contextApp = null
        restoreRequest = restoreRequest.next(FocusRestoreReason.DIALOG_DISMISSED)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TvHeaderAction(
                    text = "‹  Geri",
                    onClick = onBack,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (hiddenAppCount > 0) {
                    TvHeaderAction(
                        text = "Gizlenenler  $hiddenAppCount  ›",
                        onClick = onOpenHiddenApps,
                        onFocused = { onFocusTargetChanged(AllAppsFocusKey.HIDDEN_APPS) },
                        modifier = Modifier.focusRequester(hiddenAppsFocusRequester),
                    )
                }
            }

            Spacer(modifier = Modifier.height(SeyirSpacing.Section))
            Text(
                text = "Tüm Uygulamalar",
                fontSize = SeyirType.Hero,
                fontWeight = FontWeight.SemiBold,
                color = SeyirColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
            Text(
                text = "${apps.size} uygulama  •  Uzun OK: seçenekler  •  BACK: geri",
                fontSize = SeyirType.CardLabel,
                color = SeyirColors.TextTertiary,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Section))

            if (apps.isEmpty()) {
                Text(
                    text = "Görünür uygulama bulunamadı.",
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
                        AllAppsCard(
                            app = app,
                            isFavorite = app.packageName in favoritePackageSet,
                            onClick = { onAppClick(app) },
                            onLongClick = { contextApp = app },
                            onFocused = { onFocusTargetChanged(app.packageName) },
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

    contextApp?.let { app ->
        AppContextDialog(
            app = app,
            isFavorite = app.packageName in favoritePackageSet,
            onOpen = {
                closeDialogAndRestore()
                onAppClick(app)
            },
            onToggleFavorite = {
                closeDialogAndRestore()
                onToggleFavorite(app)
            },
            onHide = {
                val fallback = adjacentFallbackKey(
                    itemKeys = apps.map { it.packageName },
                    removedKey = app.packageName,
                )
                onFocusTargetChanged(
                    fallback ?: AllAppsFocusKey.HIDDEN_APPS,
                )
                closeDialogAndRestore()
                onHideApp(app)
            },
            onAppInfo = {
                closeDialogAndRestore()
                onOpenAppInfo(app)
            },
            onDismiss = { closeDialogAndRestore() },
        )
    }
}

private suspend fun requestAllAppsFocus(
    apps: List<InstalledApp>,
    focusTarget: String?,
    appFocusRequesters: Map<String, FocusRequester>,
    hiddenAppCount: Int,
    hiddenAppsFocusRequester: FocusRequester,
    gridState: LazyGridState,
) {
    val itemKeys = apps.map { it.packageName } +
        if (hiddenAppCount > 0) listOf(AllAppsFocusKey.HIDDEN_APPS) else emptyList()
    restoreItemFocus(
        itemKeys = itemKeys,
        focusTarget = focusTarget,
        isItemVisible = { targetIndex ->
            gridState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
        },
        scrollToItem = gridState::scrollToItem,
        requestFocus = { key ->
            if (key == AllAppsFocusKey.HIDDEN_APPS) {
                hiddenAppsFocusRequester.requestFocus()
            } else {
                appFocusRequesters[key]?.requestFocus()
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllAppsCard(
    app: InstalledApp,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .width(SeyirSize.AppCardWidth)
            .tvFocusScale(focused, "all-apps-card-scale")
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .tvDpadClick(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .focusable()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
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

            if (isFavorite) {
                Text(
                    text = "★",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 9.dp, end = 11.dp),
                    fontSize = 14.sp,
                    color = SeyirColors.Accent.copy(alpha = 0.9f),
                )
            }
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

@Composable
private fun AppContextDialog(
    app: InstalledApp,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val firstActionFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        awaitFocusLayout()
        requestFocusBestEffort { firstActionFocusRequester.requestFocus() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(SeyirRadius.Dialog))
                .background(SeyirColors.SurfaceElevated)
                .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    bitmap = app.icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = app.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SeyirColors.TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            ContextAction(
                text = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                onClick = onToggleFavorite,
                modifier = Modifier.focusRequester(firstActionFocusRequester),
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
            ContextAction(
                text = "Aç",
                onClick = onOpen,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
            ContextAction(
                text = "Gizle",
                onClick = onHide,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
            ContextAction(
                text = "Uygulama bilgisi",
                onClick = onAppInfo,
            )
        }
    }
}

@Composable
private fun ContextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvAction(
        text = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        unfocusedContainerColor = Color.Transparent,
    )
}
