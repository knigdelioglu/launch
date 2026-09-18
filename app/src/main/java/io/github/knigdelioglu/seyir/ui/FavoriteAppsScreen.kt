package io.github.knigdelioglu.seyir.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    val favoritePackageSet = remember(favoritePackageNames) { favoritePackageNames.toSet() }
    val favoriteIndexByPackage = remember(favoritePackageNames) {
        favoritePackageNames.withIndex().associate { (index, packageName) -> packageName to index }
    }
    val orderedApps = remember(apps, favoritePackageNames) {
        favoritePackageNames.mapNotNull(appByPackage::get) +
            apps.filterNot { it.packageName in favoritePackageSet }
    }
    val appFocusRequesters = remember(orderedApps.map { it.packageName }) {
        orderedApps.associate { it.packageName to FocusRequester() }
    }
    val backFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    var contextApp by remember { mutableStateOf<InstalledApp?>(null) }
    var restoreRequest by remember { mutableStateOf(FocusRestoreRequest()) }

    FocusRestoreEffect(
        itemKeys = orderedApps.map { it.packageName },
        focusTarget = focusTarget,
        request = restoreRequest,
    ) { latestFocusTarget ->
        if (orderedApps.isEmpty()) {
            requestFocusBestEffort { backFocusRequester.requestFocus() }
        } else {
            restoreItemFocus(
                itemKeys = orderedApps.map { it.packageName },
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
            TvHeaderAction(
                text = "‹  Ayarlar",
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
                        val favoriteIndex = favoriteIndexByPackage[app.packageName] ?: -1
                        val isFavorite = favoriteIndex >= 0
                        FavoriteAppCard(
                            app = app,
                            isFavorite = isFavorite,
                            favoriteIndex = favoriteIndex,
                            onClick = {
                                onFocusTargetChanged(app.packageName)
                                restoreRequest = restoreRequest.next(FocusRestoreReason.ORDER_CHANGED)
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
            TvTransientMessage(
                message = message,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    contextApp?.let { app ->
        val favoriteIndex = favoriteIndexByPackage[app.packageName] ?: -1
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
    Column(
        modifier = modifier
            .width(SeyirSize.AppCardWidth)
            .tvFocusScale(focused, "favorite-app-card-scale")
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
            )
            .semantics(mergeDescendants = true) {
                contentDescription = if (isFavorite) {
                    "${app.label}, favoride, sıra ${favoriteIndex + 1}"
                } else {
                    "${app.label}, favorilere ekle"
                }
            },
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
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = SeyirType.CardLabel,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            color = if (focused) SeyirColors.TextPrimary else SeyirColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = if (isFavorite) "Sabitlendi" else "Ekle",
            fontSize = SeyirType.Meta,
            fontWeight = FontWeight.Medium,
            color = if (focused) {
                SeyirColors.TextPrimary
            } else if (isFavorite) {
                SeyirColors.Accent
            } else {
                SeyirColors.TextTertiary
            },
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
        awaitFocusLayout()
        requestFocusBestEffort { firstActionFocusRequester.requestFocus() }
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

            FavoriteManagementAction(
                text = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                onClick = onToggleFavorite,
                modifier = Modifier.focusRequester(firstActionFocusRequester),
            )
            if (isFavorite && canMoveLeft) {
                Spacer(modifier = Modifier.height(8.dp))
                FavoriteManagementAction(
                    text = "Sola taşı",
                    onClick = onMoveLeft,
                )
            }
            if (isFavorite && canMoveRight) {
                Spacer(modifier = Modifier.height(8.dp))
                FavoriteManagementAction(
                    text = "Sağa taşı",
                    onClick = onMoveRight,
                )
            }
        }
    }
}

@Composable
private fun FavoriteManagementAction(
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
