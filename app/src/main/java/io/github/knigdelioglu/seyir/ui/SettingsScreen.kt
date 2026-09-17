package io.github.knigdelioglu.seyir.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import io.github.knigdelioglu.seyir.data.AccentMode
import io.github.knigdelioglu.seyir.data.ThemeMode
import io.github.knigdelioglu.seyir.ui.theme.SeyirColors
import io.github.knigdelioglu.seyir.ui.theme.SeyirMotion
import io.github.knigdelioglu.seyir.ui.theme.SeyirRadius
import io.github.knigdelioglu.seyir.ui.theme.SeyirSpacing
import io.github.knigdelioglu.seyir.ui.theme.SeyirType
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    visibleAppCount: Int,
    hiddenAppCount: Int,
    themeMode: ThemeMode,
    accentMode: AccentMode,
    reducedMotion: Boolean,
    sportsApiConfigured: Boolean,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onAccentModeChanged: (AccentMode) -> Unit,
    onReducedMotionChanged: (Boolean) -> Unit,
    onOpenApps: () -> Unit,
    onOpenSports: () -> Unit,
    onBack: () -> Unit,
) {
    val firstFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120)
        runCatching { firstFocusRequester.requestFocus() }
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
            SettingsBackAction(onClick = onBack)

            Spacer(modifier = Modifier.height(SeyirSpacing.Section))
            Text(
                text = "Ayarlar",
                fontSize = SeyirType.Hero,
                fontWeight = FontWeight.SemiBold,
                color = SeyirColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
            Text(
                text = "Görünümü ve launcher davranışını kumandayla yönetin.",
                fontSize = SeyirType.Subtitle,
                color = SeyirColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(SeyirSpacing.SectionLarge))

            SettingsSectionTitle("Yönetim")
            Spacer(modifier = Modifier.height(SeyirSpacing.Item))

            Row(
                horizontalArrangement = Arrangement.spacedBy(SeyirSpacing.Item),
            ) {
                SettingsActionCard(
                    title = "Uygulamaları yönet",
                    description = "$visibleAppCount görünür • $hiddenAppCount gizli",
                    symbol = "▦",
                    onClick = onOpenApps,
                    modifier = Modifier.focusRequester(firstFocusRequester),
                )
                SettingsActionCard(
                    title = "Bugün ne var",
                    description = if (sportsApiConfigured) {
                        "API-Football bağlı • günlük tek sorgu"
                    } else {
                        "Maç verisi kapalı"
                    },
                    symbol = "⚽",
                    onClick = onOpenSports,
                )
            }

            Spacer(modifier = Modifier.height(SeyirSpacing.Section))
            SettingsSectionTitle("Görünüm")
            Spacer(modifier = Modifier.height(SeyirSpacing.Item))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SeyirSpacing.Item),
            ) {
                SettingsChoiceCard(
                    title = "Tema",
                    value = if (themeMode == ThemeMode.DARK) "Koyu" else "Siyah",
                    detail = if (themeMode == ThemeMode.DARK) {
                        "Yumuşak koyu yüzey"
                    } else {
                        "Gerçek siyah arka plan"
                    },
                    symbol = "◐",
                    onClick = {
                        onThemeModeChanged(
                            if (themeMode == ThemeMode.DARK) ThemeMode.BLACK else ThemeMode.DARK,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                SettingsChoiceCard(
                    title = "Vurgu",
                    value = accentMode.label(),
                    detail = "Focus yüzeyi ve vurgu rengi",
                    symbol = "●",
                    onClick = { onAccentModeChanged(accentMode.next()) },
                    modifier = Modifier.weight(1f),
                )
                SettingsChoiceCard(
                    title = "Hareket",
                    value = if (reducedMotion) "Azaltılmış" else "Normal",
                    detail = if (reducedMotion) {
                        "Focus büyütmesi kapalı"
                    } else {
                        "160 ms focus geçişi"
                    },
                    symbol = "↔",
                    onClick = { onReducedMotionChanged(!reducedMotion) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(SeyirSpacing.Section))

            SettingsInfoCard(
                title = "Seyir",
                value = "0.1.0-dev",
                detail = "Yerel • reklamsız • telemetry yok",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Değişiklikler anında uygulanır ve cihazda saklanır.",
                fontSize = SeyirType.Meta,
                color = SeyirColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        fontSize = SeyirType.SectionTitle,
        fontWeight = FontWeight.SemiBold,
        color = SeyirColors.TextPrimary.copy(alpha = 0.9f),
    )
}

@Composable
private fun SettingsBackAction(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }

    Text(
        text = "‹  Ana ekran",
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
private fun SettingsActionCard(
    title: String,
    description: String,
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) SeyirMotion.FocusScale else 1f,
        animationSpec = tween(
            durationMillis = SeyirMotion.FocusDurationMs,
            easing = SeyirMotion.FocusEasing,
        ),
        label = "settings-action-scale",
    )

    Row(
        modifier = modifier
            .width(430.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(SeyirRadius.Card))
            .background(
                if (focused) SeyirColors.SurfaceFocused else SeyirColors.SurfaceSoft,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = symbol,
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            color = if (focused) SeyirColors.Accent else SeyirColors.TextPrimary,
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
                color = SeyirColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = SeyirType.Meta,
                color = SeyirColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun SettingsChoiceCard(
    title: String,
    value: String,
    detail: String,
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) SeyirMotion.FocusScale else 1f,
        animationSpec = tween(
            durationMillis = SeyirMotion.FocusDurationMs,
            easing = SeyirMotion.FocusEasing,
        ),
        label = "settings-choice-scale",
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(SeyirRadius.Card))
            .background(
                if (focused) SeyirColors.SurfaceFocused else SeyirColors.SurfaceSoft,
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = symbol,
                fontSize = 18.sp,
                color = if (focused) SeyirColors.Accent else SeyirColors.TextSecondary,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = SeyirType.Meta,
                fontWeight = FontWeight.Medium,
                color = SeyirColors.TextTertiary,
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = SeyirColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = detail,
            fontSize = SeyirType.Meta,
            color = SeyirColors.TextSecondary,
        )
    }
}

@Composable
private fun SettingsInfoCard(
    title: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(SeyirRadius.Card))
            .background(SeyirColors.SurfaceSoft)
            .padding(22.dp),
    ) {
        Text(
            text = title,
            fontSize = SeyirType.Meta,
            fontWeight = FontWeight.Medium,
            color = SeyirColors.TextTertiary,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = SeyirColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = detail,
            fontSize = SeyirType.Meta,
            color = SeyirColors.TextSecondary,
        )
    }
}

private fun AccentMode.label(): String = when (this) {
    AccentMode.NEUTRAL -> "Nötr"
    AccentMode.BLUE -> "Mavi"
    AccentMode.EMERALD -> "Zümrüt"
}

private fun AccentMode.next(): AccentMode = when (this) {
    AccentMode.NEUTRAL -> AccentMode.BLUE
    AccentMode.BLUE -> AccentMode.EMERALD
    AccentMode.EMERALD -> AccentMode.NEUTRAL
}
