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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
    onDismissMessage: () -> Unit,
) {
    val clock = rememberClock()
    val homeApps = uiState.favoriteApps
    val favoriteFocusRequesters = remember(homeApps.map { it.packageName }) {
        homeApps.associate { it.packageName to FocusRequester() }
    }
    val allAppsFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    var contextApp by remember { mutableStateOf<InstalledApp?>(null) }

    LaunchedEffect(homeApps, uiState.apps, focusTarget) {
        if (uiState.apps.isEmpty()) return@LaunchedEffect
        delay(140)

        val requester = when (focusTarget) {
            HomeFocusKey.ALL_APPS -> allAppsFocusRequester
            HomeFocusKey.SETTINGS -> settingsFocusRequester
            else -> favoriteFocusRequesters[focusTarget]
        } ?: homeApps.firstOrNull()?.let { favoriteFocusRequesters[it.packageName] }
            ?: allAppsFocusRequester

        runCatching { requester.requestFocus() }
    }

    LaunchedEffect(uiState.transientMessage) {
        if (uiState.transientMessage != null) {
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
            TopBar(
                clock = clock,
                settingsFocusRequester = settingsFocusRequester,
                onSettingsFocused = { onFocusTargetChanged(HomeFocusKey.SETTINGS) },
                onOpenSettings = onOpenSettings,
            )

            Spacer(modifier = Modifier.height(SeyirSpacing.SectionLarge))

            Text(
                text = greetingFor(LocalTime.now()),
                fontSize = SeyirType.Hero,
                fontWeight = FontWeight.SemiBold,
                color = SeyirColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
            Text(
                text = "İzlemek istediğiniz şeye doğrudan geçin.",
                fontSize = SeyirType.Subtitle,
                color = SeyirColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(SeyirSpacing.SectionLarge))

            when {
                uiState.apps.isNotEmpty() -> {
                    SectionHeader(
                        title = "Favoriler",
                        meta = "${homeApps.size} sabitlenmiş",
                    )
                    Spacer(modifier = Modifier.height(SeyirSpacing.Item))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(SeyirSpacing.Item),
                    ) {
                        itemsIndexed(
                            items = homeApps,
                            key = { _, app -> app.packageName },
                        ) { _, app ->
                            AppCard(
                                app = app,
                                onClick = { onAppClick(app) },
                                onLongClick = { contextApp = app },
                                onFocused = { onFocusTargetChanged(app.packageName) },
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
                                onFocused = { onFocusTargetChanged(HomeFocusKey.ALL_APPS) },
                                modifier = Modifier.focusRequester(allAppsFocusRequester),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(SeyirSpacing.Item))
                    Text(
                        text = if (homeApps.isEmpty()) {
                            "Favori yok. Tüm Uygulamalar bölümünden ekleyebilirsiniz."
                        } else {
                            "Uzun OK ile favoriyi taşıyabilir veya kaldırabilirsiniz."
                        },
                        fontSize = SeyirType.Meta,
                        color = SeyirColors.TextTertiary,
                    )
                }

                uiState.isLoading -> LoadingState()
                uiState.errorMessage != null -> ErrorState(
                    message = uiState.errorMessage,
                    onRetry = onRetry,
                )
                else -> EmptyState()
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Yerel  •  Reklamsız  •  Hesapsız",
                    fontSize = SeyirType.Meta,
                    color = SeyirColors.TextTertiary,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${uiState.apps.size} uygulama",
                    fontSize = SeyirType.Meta,
                    color = SeyirColors.TextTertiary,
                )
            }
        }

        uiState.transientMessage?.let { message ->
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
        val index = homeApps.indexOfFirst { it.packageName == app.packageName }
        FavoriteContextDialog(
            app = app,
            canMoveLeft = index > 0,
            canMoveRight = index >= 0 && index < homeApps.lastIndex,
            onMoveLeft = {
                contextApp = null
                onMoveFavorite(app, -1)
            },
            onMoveRight = {
                contextApp = null
                onMoveFavorite(app, 1)
            },
            onRemove = {
                contextApp = null
                onToggleFavorite(app)
            },
            onDismiss = { contextApp = null },
        )
    }
}

@Composable
private fun TopBar(
    clock: String,
    settingsFocusRequester: FocusRequester,
    onSettingsFocused: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "SEYİR",
                fontSize = SeyirType.Brand,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = SeyirColors.TextPrimary.copy(alpha = 0.92f),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "TV ana ekranı",
                fontSize = SeyirType.Meta,
                color = SeyirColors.TextTertiary,
            )
        }

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
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = symbol,
            fontSize = SeyirType.CardLabel,
            color = SeyirColors.TextPrimary,
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
    meta: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = SeyirType.SectionTitle,
            fontWeight = FontWeight.SemiBold,
            color = SeyirColors.TextPrimary.copy(alpha = 0.9f),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = meta,
            fontSize = SeyirType.Meta,
            color = SeyirColors.TextTertiary,
        )
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
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) SeyirMotion.FocusScale else 1f,
        animationSpec = tween(SeyirMotion.FocusDurationMs),
        label = "app-card-scale",
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
            .focusable()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
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

@Composable
private fun ActionCard(
    label: String,
    symbol: String,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) SeyirMotion.FocusScale else 1f,
        animationSpec = tween(SeyirMotion.FocusDurationMs),
        label = "action-card-scale",
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
            .focusable()
            .clickable(onClick = onClick),
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
            Text(
                text = symbol,
                fontSize = SeyirType.Hero,
                fontWeight = FontWeight.Light,
                color = if (focused) SeyirColors.TextPrimary else SeyirColors.TextSecondary,
            )
        }
        Spacer(modifier = Modifier.height(SeyirSpacing.Compact))
        Text(
            text = label,
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

            if (canMoveLeft) {
                FavoriteAction(
                    text = "Sola taşı",
                    onClick = onMoveLeft,
                    modifier = Modifier.focusRequester(firstActionFocusRequester),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (canMoveRight) {
                FavoriteAction(
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
            FavoriteAction(
                text = "Favorilerden çıkar",
                onClick = onRemove,
                modifier = if (!canMoveLeft && !canMoveRight) {
                    Modifier.focusRequester(firstActionFocusRequester)
                } else {
                    Modifier
                },
            )
        }
    }
}

@Composable
private fun FavoriteAction(
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

private fun greetingFor(time: LocalTime): String = when (time.hour) {
    in 5..11 -> "Günaydın"
    in 12..17 -> "İyi günler"
    else -> "İyi akşamlar"
}
