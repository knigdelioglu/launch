package io.github.knigdelioglu.seyir.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

object FavoriteAppsFocusKey {
    const val BACK = "__favorite_apps_back__"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteAppsScreen(
    apps: List<InstalledApp>,
    favoritePackageNames: List<String>,
    transientMessage: String?,
    focusTarget: String?,
    onFocusTargetChanged: (String) -> Unit,
    onToggleFavorite: (InstalledApp) -> Unit,
    onMoveFavorite: (InstalledApp, Int) -> Unit,
    onBack: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    val appByPackage = remember(apps) { apps.associateBy { it.packageName } }
    val orderedApps = remember(apps, favoritePackageNames) {
        favoritePackageNames.mapNotNull(appByPackage::get) +
            apps.filterNot { it.packageName in favoritePackageNames }
    }
    val appFocusRequesters = remember(orderedApps.map { it.packageName }) {
        orderedApps.associate { it.packageName to FocusRequester() }
    }
    val backFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    var contextApp by remember { mutableStateOf<InstalledApp?>(null) }
    var restoreRequest by remember { mutableIntStateOf(0) }

    LaunchedEffect(orderedApps, focusTarget, restoreRequest) {
        delay(if (restoreRequest > 0) 90 else 120)

        if (orderedApps.isEmpty()) {
            runCatching { backFocusRequester.requestFocus() }
            return@LaunchedEffect
        }

        val targetIndex = orderedApps.indexOfFirst { it.packageName == focusTarget }
            .takeIf { it >= 0 } ?: 0
        runCatching { gridState.scrollToItem(targetIndex) }
        delay(16)
        orderedApps[targetIndex].packageName.let { packageName ->
            appFocusRequesters[packageName]?.let { requester ->
                runCatching { requester.requestFocus() }
            }
        }
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
            FavoriteAppsBackAction(
                onClick = onBack,
                modifier = Modifier.focusRequester(backFocusRequester),
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Section))

            Text(
                text = "Favorileri yönet",
                fontSize = SeyirType.Hero,
                fontWeight = FontWeight.SemiBold,
                color = SeyirColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
            Text(
                text = "Bir uygulamayı seçerek ana ekrandaki favori durumunu değiştirin.",
                fontSize = SeyirType.Subtitle,
                color = SeyirColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
            Text(
                text = "${favoritePackageNames.count { it in appByPackage }} favori  •  OK: ekle/çıkar  •  Uzun OK: sırala",
                fontSize = SeyirType.CardLabel,
                color = SeyirColors.TextTertiary,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Section))

            if (orderedApps.isEmpty()) {
                Text(
                    text = "Yönetilecek uygulama bulunamadı.",
                    color = SeyirColors.TextSecondary,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = SeyirSpacing.Section),
                    horizontalArrangement = Arrangement.spacedBy(SeyirSpacing.Item),
                    verticalArrangement = Arrangement.spacedBy(SeyirSpacing.Section),
                ) {
                    itemsIndexed(
                        items = orderedApps,
                        key = { _, app -> app.packageName },
                    ) { _, app ->
                        val isFavorite = app.packageName in favoritePackageNames
                        val favoriteIndex = favoritePackageNames.indexOf(app.packageName)
                        FavoriteAppCard(
                            app = app,
                            isFavorite = isFavorite,
                            favoriteIndex = favoriteIndex,
                            onClick = {
                                onFocusTargetChanged(app.packageName)
                                onToggleFavorite(app)
                            },
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

    contextApp?.let { app ->
        val favoriteIndex = favoritePackageNames.indexOf(app.packageName)
        FavoriteAppContextDialog(
            app = app,
            isFavorite = favoriteIndex >= 0,
            canMoveLeft = favoriteIndex > 0,
            canMoveRight = favoriteIndex >= 0 && favoriteIndex < favoritePackageNames.lastIndex,
            onMoveLeft = {
                onFocusTargetChanged(app.packageName)
                closeDialogAndRestore()
                onMoveFavorite(app, -1)
            },
            onMoveRight = {
                onFocusTargetChanged(app.packageName)
                closeDialogAndRestore()
                onMoveFavorite(app, 1)
            },
            onToggleFavorite = {
                onFocusTargetChanged(app.packageName)
                closeDialogAndRestore()
                onToggleFavorite(app)
            },
            onDismiss = { closeDialogAndRestore() },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteAppCard(
    app: InstalledApp,
    isFavorite: Boolean,
    favoriteIndex: Int,
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
        label = "favorite-app-card-scale",
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
            )
            .semantics(mergeDescendants = true) {
                contentDescription = if (isFavorite) {
                    "${app.label}, favoride, sıra ${favoriteIndex + 1}"
                } else {
                    "${app.label}, favorilere ekle"
                }
            },
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
                contentDescription = null,
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
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = if (isFavorite) "Favoride • ${favoriteIndex + 1}" else "Favorilere ekle",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = SeyirType.Meta,
            color = if (isFavorite) SeyirColors.Accent else SeyirColors.TextTertiary,
        )
    }
}

@Composable
private fun FavoriteAppContextDialog(
    app: InstalledApp,
    isFavorite: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onToggleFavorite: () -> Unit,
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
                .width(440.dp)
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
                Column {
                    Text(
                        text = app.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SeyirColors.TextPrimary,
                    )
                    Text(
                        text = if (isFavorite) "Favori uygulama" else "Favori değil",
                        fontSize = SeyirType.Meta,
                        color = SeyirColors.TextSecondary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(22.dp))

            if (isFavorite && canMoveLeft) {
                FavoriteManagementAction(
                    text = "Sola taşı",
                    onClick = onMoveLeft,
                    modifier = Modifier.focusRequester(firstActionFocusRequester),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isFavorite && canMoveRight) {
                FavoriteManagementAction(
                    text = "Sağa taşı",
                    onClick = onMoveRight,
                    modifier = if (!canMoveLeft) {
                        Modifier.focusRequester(firstActionFocusRequester)
                    } else {
                        Modifier
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            FavoriteManagementAction(
                text = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                onClick = onToggleFavorite,
                modifier = if (!isFavorite || (!canMoveLeft && !canMoveRight)) {
                    Modifier.focusRequester(firstActionFocusRequester)
                } else {
                    Modifier
                },
            )
        }
    }
}

@Composable
private fun FavoriteAppsBackAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }

    Text(
        text = "‹  Ayarlar",
        modifier = modifier
            .clip(RoundedCornerShape(SeyirRadius.Pill))
            .background(
                if (focused) SeyirColors.SurfaceFocused else SeyirColors.SurfaceSoft,
            )
            .onFocusChanged { focused = it.isFocused }
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
private fun FavoriteManagementAction(
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
