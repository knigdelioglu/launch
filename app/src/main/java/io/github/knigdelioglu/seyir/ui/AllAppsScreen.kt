package io.github.knigdelioglu.seyir.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import io.github.knigdelioglu.seyir.data.InstalledApp
import io.github.knigdelioglu.seyir.ui.theme.SeyirColors
import io.github.knigdelioglu.seyir.ui.theme.SeyirMotion
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
    val appFocusRequesters = remember(apps.map { it.packageName }) {
        apps.associate { it.packageName to FocusRequester() }
    }
    val hiddenAppsFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    var contextApp by remember { mutableStateOf<InstalledApp?>(null) }
    var restoreRequest by remember { mutableIntStateOf(0) }

    // focusTarget is history only; normal D-pad navigation must not restart focus restoration.
    LaunchedEffect(apps, hiddenAppCount, restoreRequest) {
        if (apps.isEmpty() && hiddenAppCount == 0) return@LaunchedEffect
        delay(if (restoreRequest > 0) 90 else 120)
        requestAllAppsFocus(
            apps = apps,
            focusTarget = focusTarget,
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
        restoreRequest += 1
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
                HeaderAction(
                    text = "‹  Geri",
                    onClick = onBack,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (hiddenAppCount > 0) {
                    HeaderAction(
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
                            isFavorite = app.packageName in favoritePackageNames,
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
            TransientMessage(
                message = message,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    contextApp?.let { app ->
        AppContextDialog(
            app = app,
            isFavorite = app.packageName in favoritePackageNames,
            onOpen = {
                closeDialogAndRestore()
                onAppClick(app)
            },
            onToggleFavorite = {
                closeDialogAndRestore()
                onToggleFavorite(app)
            },
            onHide = {
                val currentIndex = apps.indexOfFirst { it.packageName == app.packageName }
                val fallback = apps.getOrNull(currentIndex + 1)
                    ?: apps.getOrNull(currentIndex - 1)
                onFocusTargetChanged(
                    fallback?.packageName ?: AllAppsFocusKey.HIDDEN_APPS,
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
    if (focusTarget == AllAppsFocusKey.HIDDEN_APPS && hiddenAppCount > 0) {
        runCatching { hiddenAppsFocusRequester.requestFocus() }
        return
    }

    val targetIndex = when {
        focusTarget != null -> apps.indexOfFirst { it.packageName == focusTarget }
        else -> -1
    }.takeIf { it >= 0 } ?: if (apps.isNotEmpty()) 0 else -1

    if (targetIndex >= 0) {
        runCatching { gridState.scrollToItem(targetIndex) }
        delay(16)
        val packageName = apps[targetIndex].packageName
        appFocusRequesters[packageName]?.let { requester ->
            runCatching { requester.requestFocus() }
        }
        return
    }

    if (hiddenAppCount > 0) {
        runCatching { hiddenAppsFocusRequester.requestFocus() }
    }
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
    val scale by animateFloatAsState(
        targetValue = if (focused) SeyirMotion.FocusScale else 1f,
        animationSpec = tween(
            durationMillis = SeyirMotion.FocusDurationMs,
            easing = SeyirMotion.FocusEasing,
        ),
        label = "all-apps-card-scale",
    )

    Column(
        modifier = modifier
            .width(SeyirSize.AppCardWidth)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .tvDpadClick(onClick = onClick)
            .focusable()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
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
        delay(80)
        runCatching { firstActionFocusRequester.requestFocus() }
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
                text = "Aç",
                onClick = onOpen,
                modifier = Modifier.focusRequester(firstActionFocusRequester),
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
            ContextAction(
                text = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                onClick = onToggleFavorite,
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
private fun HeaderAction(
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

@Composable
private fun ContextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SeyirRadius.Action))
            .background(
                if (focused) SeyirColors.SurfaceFocused else Color.Transparent,
            )
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick(onClick = onClick)
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Text(
            text = text,
            fontSize = SeyirType.CardLabel,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            color = if (focused) SeyirColors.TextPrimary else SeyirColors.TextSecondary,
        )
    }
}

@Composable
private fun TransientMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
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
