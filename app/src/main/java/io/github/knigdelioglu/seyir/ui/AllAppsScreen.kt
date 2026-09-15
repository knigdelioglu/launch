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
import kotlinx.coroutines.delay

@Composable
fun AllAppsScreen(
    apps: List<InstalledApp>,
    favoritePackageNames: List<String>,
    transientMessage: String?,
    onAppClick: (InstalledApp) -> Unit,
    onToggleFavorite: (InstalledApp) -> Unit,
    onBack: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    val firstFocusRequester = remember { FocusRequester() }
    var contextApp by remember { mutableStateOf<InstalledApp?>(null) }

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
            Text(
                text = "‹  Ana ekran",
                modifier = Modifier
                    .focusable()
                    .clickable(onClick = onBack),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.62f),
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Tüm Uygulamalar",
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${apps.size} uygulama  •  Uzun OK: seçenekler  •  BACK: ana ekran",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.52f),
            )
            Spacer(modifier = Modifier.height(30.dp))

            if (apps.isEmpty()) {
                Text(
                    text = "Açılabilir uygulama bulunamadı.",
                    color = Color.White.copy(alpha = 0.62f),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    itemsIndexed(
                        items = apps,
                        key = { _, app -> app.packageName },
                    ) { index, app ->
                        AllAppsCard(
                            app = app,
                            isFavorite = app.packageName in favoritePackageNames,
                            onClick = { onAppClick(app) },
                            onLongClick = { contextApp = app },
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

    contextApp?.let { app ->
        AppContextDialog(
            app = app,
            isFavorite = app.packageName in favoritePackageNames,
            onOpen = {
                contextApp = null
                onAppClick(app)
            },
            onToggleFavorite = {
                contextApp = null
                onToggleFavorite(app)
            },
            onDismiss = { contextApp = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllAppsCard(
    app: InstalledApp,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        animationSpec = tween(160),
        label = "all-apps-card-scale",
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .size(width = 168.dp, height = 102.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (focused) Color.White.copy(alpha = 0.15f)
                    else Color.White.copy(alpha = 0.065f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(58.dp),
            )

            if (isFavorite) {
                Text(
                    text = "★",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 10.dp),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.82f),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = app.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            color = Color.White.copy(alpha = if (focused) 1f else 0.76f),
        )
    }
}

@Composable
private fun AppContextDialog(
    app: InstalledApp,
    isFavorite: Boolean,
    onOpen: () -> Unit,
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
                .width(420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1B1B21))
                .padding(24.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            ContextAction(
                text = "Aç",
                onClick = onOpen,
                modifier = Modifier.focusRequester(firstActionFocusRequester),
            )
            Spacer(modifier = Modifier.height(8.dp))
            ContextAction(
                text = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                onClick = onToggleFavorite,
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
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (focused) Color.White.copy(alpha = 0.15f)
                else Color.Transparent,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            color = Color.White.copy(alpha = if (focused) 1f else 0.78f),
        )
    }
}
