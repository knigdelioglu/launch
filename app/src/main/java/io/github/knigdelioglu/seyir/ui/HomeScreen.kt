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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import io.github.knigdelioglu.seyir.data.InstalledApp
import io.github.knigdelioglu.seyir.data.TodayMatch
import io.github.knigdelioglu.seyir.ui.theme.SeyirColors
import io.github.knigdelioglu.seyir.ui.theme.SeyirRadius
import io.github.knigdelioglu.seyir.ui.theme.SeyirSize
import io.github.knigdelioglu.seyir.ui.theme.SeyirSpacing
import io.github.knigdelioglu.seyir.ui.theme.SeyirType
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object HomeFocusKey {
    const val ALL_APPS = "__all_apps__"
    const val SETTINGS = "__settings__"
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    focusTarget: String?,
    onFocusTargetChanged: (String) -> Unit,
    onAppClick: (InstalledApp) -> Unit,
    onMoveFavorite: (InstalledApp, Int) -> Unit,
    onToggleFavorite: (InstalledApp) -> Unit,
    onOpenAllApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onRefreshMatches: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    val clock = rememberClock()
    val homeApps = uiState.favoriteApps
    val favoriteFocusRequesters = remember(homeApps.map { it.packageName }) {
        homeApps.associate { it.packageName to FocusRequester() }
    }
    val allAppsFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    val favoritesListState = rememberLazyListState()
    var contextApp by remember { mutableStateOf<InstalledApp?>(null) }
    var restoreRequest by remember { mutableStateOf(FocusRestoreRequest()) }
    var initialFocusLanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(homeApps.map { it.packageName }) {
        if (!initialFocusLanded && homeApps.isNotEmpty()) {
            awaitFocusLayout()
            val firstPackage = homeApps.first().packageName
            favoriteFocusRequesters[firstPackage]?.let { requester ->
                requestFocusBestEffort {
                    requester.requestFocus()
                    initialFocusLanded = true
                    onFocusTargetChanged(firstPackage)
                }
            }
        }
    }

    val effectiveFocusTarget = if (!initialFocusLanded && focusTarget == HomeFocusKey.SETTINGS) {
        null
    } else {
        focusTarget
    }

    FocusRestoreEffect(
        itemKeys = if (uiState.apps.isEmpty()) {
            emptyList()
        } else {
            homeApps.map { it.packageName } + HomeFocusKey.ALL_APPS
        },
        focusTarget = effectiveFocusTarget,
        request = restoreRequest,
    ) { latestFocusTarget ->
        requestHomeFocus(
            homeApps = homeApps,
            focusTarget = latestFocusTarget,
            favoriteFocusRequesters = favoriteFocusRequesters,
            allAppsFocusRequester = allAppsFocusRequester,
            settingsFocusRequester = settingsFocusRequester,
            favoritesListState = favoritesListState,
        )
    }

    LaunchedEffect(uiState.transientMessage) {
        if (uiState.transientMessage != null) {
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
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = SeyirSpacing.ScreenVertical,
                ),
        ) {
            TopBar(
                clock = clock,
                settingsFocusRequester = settingsFocusRequester,
                onSettingsFocused = {
                    if (initialFocusLanded) {
                        onFocusTargetChanged(HomeFocusKey.SETTINGS)
                    }
                },
                onOpenSettings = onOpenSettings,
                modifier = Modifier.padding(horizontal = SeyirSpacing.ScreenHorizontal),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
            ) {
                Spacer(modifier = Modifier.height(SeyirSpacing.Compact))

                when {
                    uiState.apps.isNotEmpty() -> {
                        SectionHeader(
                            title = "Favoriler",
                            modifier = Modifier.padding(horizontal = SeyirSpacing.ScreenHorizontal),
                        )
                        Spacer(modifier = Modifier.height(SeyirSpacing.Compact))

                        LazyRow(
                            state = favoritesListState,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(
                                start = SeyirSpacing.ScreenHorizontal,
                                end = SeyirSpacing.ScreenHorizontal,
                                top = 8.dp,
                                bottom = 8.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(SeyirSpacing.Item),
                        ) {
                            itemsIndexed(
                                items = homeApps,
                                key = { _, app -> app.packageName },
                            ) { index, app ->
                                AppCard(
                                    app = app,
                                    isFirst = index == 0,
                                    onClick = { onAppClick(app) },
                                    onLongClick = { contextApp = app },
                                    onFocused = {
                                        initialFocusLanded = true
                                        onFocusTargetChanged(app.packageName)
                                    },
                                    modifier = Modifier.focusRequester(
                                        favoriteFocusRequesters.getValue(app.packageName),
                                    ),
                                )
                            }

                            item(key = HomeFocusKey.ALL_APPS) {
                                ActionCard(
                                    label = "Tüm Uygulamalar",
                                    symbol = "▦",
                                    onClick = onOpenAllApps,
                                    onFocused = {
                                        initialFocusLanded = true
                                        onFocusTargetChanged(HomeFocusKey.ALL_APPS)
                                    },
                                    modifier = Modifier.focusRequester(allAppsFocusRequester),
                                )
                            }
                        }

                        if (homeApps.isEmpty()) {
                            Spacer(modifier = Modifier.height(SeyirSpacing.Compact))
                            Text(
                                text = "Favori yok. Tüm Uygulamalar bölümünden ekleyebilirsiniz.",
                                fontSize = SeyirType.Meta,
                                color = SeyirColors.TextTertiary,
                                modifier = Modifier.padding(horizontal = SeyirSpacing.ScreenHorizontal),
                            )
                        }

                        if (uiState.sportsApiConfigured) {
                            Spacer(modifier = Modifier.height(SeyirSpacing.Item))
                            TodayMatchesSection(
                                matches = uiState.todayMatches,
                                loading = uiState.matchesLoading,
                                error = uiState.matchesError,
                                onRefresh = onRefreshMatches,
                            )
                        }
                    }

                    uiState.isLoading -> Box(modifier = Modifier.padding(horizontal = SeyirSpacing.ScreenHorizontal)) {
                        LoadingState()
                    }
                    uiState.errorMessage != null -> Box(modifier = Modifier.padding(horizontal = SeyirSpacing.ScreenHorizontal)) {
                        ErrorState(
                            message = uiState.errorMessage,
                            onRetry = onRetry,
                        )
                    }
                    else -> Box(modifier = Modifier.padding(horizontal = SeyirSpacing.ScreenHorizontal)) {
                        EmptyState()
                    }
                }

                Spacer(modifier = Modifier.height(SeyirSpacing.Compact))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SeyirSpacing.ScreenHorizontal),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${uiState.apps.size} uygulama",
                        fontSize = SeyirType.Meta,
                        color = SeyirColors.TextTertiary,
                    )
                }
            }
        }

        uiState.transientMessage?.let { message ->
            TvTransientMessage(
                message = message,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    contextApp?.let { app ->
        val index = homeApps.indexOfFirst { it.packageName == app.packageName }
        FavoriteContextDialog(
            app = app,
            canMoveLeft = index > 0,
            canMoveRight = index >= 0 && index < homeApps.lastIndex,
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
            onRemove = {
                val fallback = adjacentFallbackKey(
                    itemKeys = homeApps.map { it.packageName },
                    removedKey = app.packageName,
                )
                onFocusTargetChanged(fallback ?: HomeFocusKey.ALL_APPS)
                closeDialogAndRestore()
                onToggleFavorite(app)
            },
            onDismiss = { closeDialogAndRestore() },
        )
    }
}

@Composable
private fun TodayMatchesSection(
    matches: List<TodayMatch>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
) {
    var nowEpochSeconds by remember { mutableStateOf(System.currentTimeMillis() / 1_000L) }

    LaunchedEffect(Unit) {
        while (true) {
            nowEpochSeconds = System.currentTimeMillis() / 1_000L
            delay(30_000)
        }
    }

    val visibleMatches = remember(matches, nowEpochSeconds) {
        selectCurrentAndUpcomingMatches(
            matches = matches,
            nowEpochSeconds = nowEpochSeconds,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SeyirSpacing.ScreenHorizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Bugün ne var",
            fontSize = SeyirType.SectionTitle,
            fontWeight = FontWeight.SemiBold,
            color = SeyirColors.TextPrimary.copy(alpha = 0.9f),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = when {
                loading -> "yükleniyor"
                error != null -> "veri alınamadı"
                visibleMatches.isEmpty() -> "kalan maç yok"
                else -> "${visibleMatches.size} maç • Türkiye saati"
            },
            fontSize = SeyirType.Meta,
            color = SeyirColors.TextTertiary,
        )
        Spacer(modifier = Modifier.weight(1f))
        MatchRefreshAction(onClick = onRefresh)
    }

    Spacer(modifier = Modifier.height(SeyirSpacing.Compact))

    when {
        loading && visibleMatches.isEmpty() -> Text(
            text = "Maçlar hazırlanıyor…",
            fontSize = SeyirType.Meta,
            color = SeyirColors.TextSecondary,
            modifier = Modifier.padding(horizontal = SeyirSpacing.ScreenHorizontal),
        )

        error != null && visibleMatches.isEmpty() -> Text(
            text = error,
            fontSize = SeyirType.Meta,
            color = SeyirColors.TextSecondary,
            modifier = Modifier.padding(horizontal = SeyirSpacing.ScreenHorizontal),
        )

        visibleMatches.isEmpty() -> Text(
            text = "Şu anda oynanan veya daha sonra başlayacak maç bulunamadı.",
            fontSize = SeyirType.Meta,
            color = SeyirColors.TextSecondary,
            modifier = Modifier.padding(horizontal = SeyirSpacing.ScreenHorizontal),
        )

        else -> LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SeyirSpacing.Compact),
            contentPadding = PaddingValues(
                start = SeyirSpacing.ScreenHorizontal,
                end = SeyirSpacing.ScreenHorizontal,
                top = 4.dp,
                bottom = 4.dp,
            ),
        ) {
            itemsIndexed(
                items = visibleMatches,
                key = { _, match -> match.fixtureId },
            ) { index, match ->
                TodayMatchCard(
                    match = match,
                    isFirst = index == 0,
                )
            }
        }
    }
}

@Composable
private fun TodayMatchCard(
    match: TodayMatch,
    isFirst: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(214.dp)
            .tvFocusScale(
                focused = focused,
                label = "today-match-card-scale",
                scaleOrigin = if (isFirst) TransformOrigin(0f, 0.5f) else TransformOrigin.Center,
            )
            .background(
                color = if (focused) SeyirColors.SurfaceFocused else SeyirColors.SurfaceSoft,
                shape = RoundedCornerShape(SeyirRadius.Action),
            )
            .tvFocusedChasingBorder(
                focused = focused,
                cornerRadius = SeyirRadius.Action,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(horizontal = 15.dp, vertical = 11.dp),
    ) {
        Text(
            text = match.leagueName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp,
            color = SeyirColors.TextTertiary,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = match.homeTeam,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = SeyirType.Meta,
            fontWeight = FontWeight.Medium,
            color = SeyirColors.TextPrimary,
        )
        Text(
            text = match.awayTeam,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = SeyirType.Meta,
            fontWeight = FontWeight.Medium,
            color = SeyirColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = formatMatchScheduleText(match),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (focused) SeyirColors.TextPrimary else SeyirColors.TextSecondary,
        )
    }
}

@Composable
private fun MatchRefreshAction(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }

    Text(
        text = "Yenile",
        modifier = Modifier
            .clip(RoundedCornerShape(SeyirRadius.Pill))
            .background(
                if (focused) SeyirColors.SurfaceFocused else Color.Transparent,
            )
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick(onClick = onClick)
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        fontSize = SeyirType.Meta,
        fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
        color = if (focused) SeyirColors.TextPrimary else SeyirColors.TextTertiary,
    )
}

private suspend fun requestHomeFocus(
    homeApps: List<InstalledApp>,
    focusTarget: String?,
    favoriteFocusRequesters: Map<String, FocusRequester>,
    allAppsFocusRequester: FocusRequester,
    settingsFocusRequester: FocusRequester,
    favoritesListState: LazyListState,
) {
    if (focusTarget == HomeFocusKey.SETTINGS) {
        requestFocusBestEffort { settingsFocusRequester.requestFocus() }
        return
    }

    val itemKeys = homeApps.map { it.packageName } + HomeFocusKey.ALL_APPS
    restoreItemFocus(
        itemKeys = itemKeys,
        focusTarget = focusTarget ?: homeApps.firstOrNull()?.packageName,
        isItemVisible = { targetIndex ->
            favoritesListState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
        },
        scrollToItem = favoritesListState::scrollToItem,
        requestFocus = { key ->
            if (key == HomeFocusKey.ALL_APPS) {
                allAppsFocusRequester.requestFocus()
            } else {
                favoriteFocusRequesters[key]?.requestFocus()
            }
        },
    )
}

@Composable
private fun TopBar(
    clock: String,
    settingsFocusRequester: FocusRequester,
    onSettingsFocused: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        TopBarAction(
            label = "Ayarlar",
            symbol = "⚙",
            onClick = onOpenSettings,
            onFocused = onSettingsFocused,
            modifier = Modifier.focusRequester(settingsFocusRequester),
        )
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = clock,
            fontSize = SeyirType.Clock,
            fontWeight = FontWeight.Medium,
            color = SeyirColors.TextSecondary,
        )
    }
}

@Composable
private fun TopBarAction(
    label: String,
    symbol: String,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(SeyirRadius.Pill))
            .background(
                if (focused) SeyirColors.SurfaceFocused else SeyirColors.SurfaceSoft,
            )
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .tvDpadClick(onClick = onClick)
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = symbol,
            fontSize = SeyirType.CardLabel,
            color = if (focused) SeyirColors.Accent else SeyirColors.TextPrimary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = SeyirType.Meta,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
            color = if (focused) SeyirColors.TextPrimary else SeyirColors.TextSecondary,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    meta: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = SeyirType.SectionTitle,
            fontWeight = FontWeight.SemiBold,
            color = SeyirColors.TextPrimary.copy(alpha = 0.9f),
        )
        if (!meta.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = meta,
                fontSize = SeyirType.Meta,
                color = SeyirColors.TextTertiary,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCard(
    app: InstalledApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    isFirst: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .width(SeyirSize.AppCardWidth)
            .tvFocusScale(
                focused = focused,
                label = "app-card-scale",
                scaleOrigin = if (isFirst) TransformOrigin(0f, 0.5f) else TransformOrigin.Center,
            )
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
private fun ActionCard(
    label: String,
    symbol: String,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .width(SeyirSize.AppCardWidth)
            .tvFocusScale(focused, "action-card-scale")
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .tvDpadClick(onClick = onClick)
            .focusable()
            .clickable(onClick = onClick),
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
            Text(
                text = symbol,
                fontSize = SeyirType.Hero,
                fontWeight = FontWeight.Light,
                color = if (focused) SeyirColors.Accent else SeyirColors.TextSecondary,
            )
        }
        Spacer(modifier = Modifier.height(SeyirSpacing.Compact))
        Text(
            text = label,
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
private fun FavoriteContextDialog(
    app: InstalledApp,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRemove: () -> Unit,
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

            FavoriteAction(
                text = "Favorilerden çıkar",
                onClick = onRemove,
                modifier = Modifier.focusRequester(firstActionFocusRequester),
            )
            if (canMoveLeft) {
                Spacer(modifier = Modifier.height(8.dp))
                FavoriteAction(
                    text = "Sola taşı",
                    onClick = onMoveLeft,
                )
            }
            if (canMoveRight) {
                Spacer(modifier = Modifier.height(8.dp))
                FavoriteAction(
                    text = "Sağa taşı",
                    onClick = onMoveRight,
                )
            }
        }
    }
}

@Composable
private fun FavoriteAction(
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

@Composable
private fun LoadingState() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = SeyirColors.TextSecondary,
            strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = "Uygulamalar hazırlanıyor…",
            color = SeyirColors.TextSecondary,
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Text(
        text = "$message  Yeniden denemek için OK tuşuna basın.",
        modifier = Modifier
            .tvDpadClick(onClick = onRetry)
            .focusable()
            .clickable(onClick = onRetry),
        color = SeyirColors.TextSecondary,
    )
}

@Composable
private fun EmptyState() {
    Text(
        text = "Açılabilir uygulama bulunamadı.",
        color = SeyirColors.TextSecondary,
    )
}

@Composable
private fun rememberClock(): String {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(30_000)
        }
    }

    return remember(now) {
        now.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}
