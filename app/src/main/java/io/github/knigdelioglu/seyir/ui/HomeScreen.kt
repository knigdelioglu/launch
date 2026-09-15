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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.knigdelioglu.seyir.data.InstalledApp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAppClick: (InstalledApp) -> Unit,
    onOpenAllApps: () -> Unit,
    onRetry: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    val firstAppFocusRequester = remember { FocusRequester() }
    val allAppsFocusRequester = remember { FocusRequester() }
    val clock = rememberClock()
    val homeApps = uiState.favoriteApps

    LaunchedEffect(homeApps, uiState.apps) {
        if (uiState.apps.isEmpty()) return@LaunchedEffect

        delay(150)
        if (homeApps.isNotEmpty()) {
            runCatching { firstAppFocusRequester.requestFocus() }
        } else {
            runCatching { allAppsFocusRequester.requestFocus() }
        }
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
                    colors = listOf(
                        Color(0xFF111116),
                        Color(0xFF09090C),
                        Color(0xFF050507),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp, vertical = 42.dp),
        ) {
            TopBar(clock = clock)
            Spacer(modifier = Modifier.height(72.dp))

            Text(
                text = greetingFor(LocalTime.now()),
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 38.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ne izlemek istersiniz?",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.58f),
            )
            Spacer(modifier = Modifier.height(42.dp))

            when {
                uiState.apps.isNotEmpty() -> {
                    Text(
                        text = "Favoriler",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        itemsIndexed(
                            items = homeApps,
                            key = { _, app -> app.packageName },
                        ) { index, app ->
                            AppCard(
                                app = app,
                                onClick = { onAppClick(app) },
                                modifier = if (index == 0) {
                                    Modifier.focusRequester(firstAppFocusRequester)
                                } else {
                                    Modifier
                                },
                            )
                        }

                        item(key = "all-apps") {
                            ActionCard(
                                label = "Tüm Uygulamalar",
                                symbol = "•••",
                                onClick = onOpenAllApps,
                                modifier = if (homeApps.isEmpty()) {
                                    Modifier.focusRequester(allAppsFocusRequester)
                                } else {
                                    Modifier
                                },
                            )
                        }
                    }

                    if (homeApps.isEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Favori uygulama yok. Tüm Uygulamalar bölümünden ekleyebilirsiniz.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.46f),
                        )
                    }
                }

                uiState.isLoading -> LoadingState()
                uiState.errorMessage != null -> ErrorState(
                    message = uiState.errorMessage,
                    onRetry = onRetry,
                )
                else -> EmptyState()
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Yerel  •  Reklamsız  •  Seyir",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.34f),
            )
        }

        uiState.transientMessage?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xEE24242B))
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.92f),
                )
            }
        }
    }
}

@Composable
private fun TopBar(clock: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "SEYİR",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = Color.White.copy(alpha = 0.88f),
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = clock,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.78f),
        )
    }
}

@Composable
private fun AppCard(
    app: InstalledApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "app-card-scale",
    )

    Column(
        modifier = modifier
            .width(168.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .size(width = 168.dp, height = 102.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (focused) {
                        Color.White.copy(alpha = 0.14f)
                    } else {
                        Color.White.copy(alpha = 0.065f)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(60.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = app.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 15.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            color = Color.White.copy(alpha = if (focused) 1f else 0.76f),
        )
    }
}

@Composable
private fun ActionCard(
    label: String,
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "action-card-scale",
    )

    Column(
        modifier = modifier
            .width(168.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .size(width = 168.dp, height = 102.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (focused) Color.White.copy(alpha = 0.16f)
                    else Color.White.copy(alpha = 0.05f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = symbol,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = Color.White.copy(alpha = if (focused) 1f else 0.68f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 15.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            color = Color.White.copy(alpha = if (focused) 1f else 0.76f),
        )
    }
}

@Composable
private fun LoadingState() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = Color.White.copy(alpha = 0.75f),
            strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = "Uygulamalar hazırlanıyor…",
            color = Color.White.copy(alpha = 0.62f),
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
        color = Color.White.copy(alpha = 0.72f),
    )
}

@Composable
private fun EmptyState() {
    Text(
        text = "Açılabilir uygulama bulunamadı.",
        color = Color.White.copy(alpha = 0.62f),
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
