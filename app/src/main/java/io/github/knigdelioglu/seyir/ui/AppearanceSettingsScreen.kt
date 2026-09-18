package io.github.knigdelioglu.seyir.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.knigdelioglu.seyir.data.ThemeMode
import io.github.knigdelioglu.seyir.ui.theme.SeyirColors
import io.github.knigdelioglu.seyir.ui.theme.SeyirRadius
import io.github.knigdelioglu.seyir.ui.theme.SeyirSpacing
import io.github.knigdelioglu.seyir.ui.theme.SeyirType

@Composable
fun AppearanceSettingsScreen(
    manualThemeMode: ThemeMode,
    scheduleEnabled: Boolean,
    startMinutes: Int,
    endMinutes: Int,
    onManualDarkModeChanged: (Boolean) -> Unit,
    onScheduleChanged: (Boolean, Int, Int) -> Unit,
    onBack: () -> Unit,
) {
    val firstFocusRequester = remember { FocusRequester() }
    var startText by remember(startMinutes) { mutableStateOf(formatMinuteOfDay(startMinutes)) }
    var endText by remember(endMinutes) { mutableStateOf(formatMinuteOfDay(endMinutes)) }

    val parsedStart = parseMinuteOfDay(startText)
    val parsedEnd = parseMinuteOfDay(endText)
    val timesValid = parsedStart != null && parsedEnd != null
    val manualDarkEnabled = manualThemeMode == ThemeMode.BLACK

    LaunchedEffect(Unit) {
        awaitFocusLayout()
        requestFocusBestEffort { firstFocusRequester.requestFocus() }
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
                text = "‹  Ana ekran",
                onClick = onBack,
            )

            Spacer(modifier = Modifier.height(SeyirSpacing.Section))
            Text(
                text = "Görünüm ayarları",
                fontSize = SeyirType.Hero,
                fontWeight = FontWeight.SemiBold,
                color = SeyirColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
            Text(
                text = "Koyu modu manuel kullanın veya belirlediğiniz saatlerde otomatik açın.",
                fontSize = SeyirType.Subtitle,
                color = SeyirColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(SeyirSpacing.SectionLarge))

            Column(
                modifier = Modifier
                    .width(860.dp)
                    .clip(RoundedCornerShape(SeyirRadius.Card))
                    .background(SeyirColors.SurfaceSoft)
                    .padding(22.dp),
            ) {
                Text(
                    text = "Koyu mod",
                    fontSize = SeyirType.SectionTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = SeyirColors.TextPrimary,
                )
                Spacer(modifier = Modifier.height(SeyirSpacing.Item))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SeyirSpacing.Item),
                ) {
                    TvAction(
                        text = if (manualDarkEnabled) {
                            "Manuel koyu mod: Açık"
                        } else {
                            "Manuel koyu mod: Kapalı"
                        },
                        onClick = { onManualDarkModeChanged(!manualDarkEnabled) },
                        modifier = Modifier.focusRequester(firstFocusRequester),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp),
                        unfocusedFontWeight = FontWeight.Medium,
                    )
                    TvAction(
                        text = if (scheduleEnabled) {
                            "Otomatik zamanlama: Açık"
                        } else {
                            "Otomatik zamanlama: Kapalı"
                        },
                        onClick = {
                            onScheduleChanged(
                                !scheduleEnabled,
                                parsedStart ?: startMinutes,
                                parsedEnd ?: endMinutes,
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp),
                        unfocusedFontWeight = FontWeight.Medium,
                    )
                }

                Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
                Text(
                    text = if (scheduleEnabled) {
                        "Zamanlama açıkken saat aralığı manuel tercihin önüne geçer."
                    } else {
                        "Zamanlama kapalıyken manuel koyu mod tercihi kullanılır."
                    },
                    fontSize = SeyirType.Meta,
                    color = SeyirColors.TextTertiary,
                )

                Spacer(modifier = Modifier.height(SeyirSpacing.Section))
                Text(
                    text = "Otomatik saat aralığı",
                    fontSize = SeyirType.SectionTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = SeyirColors.TextPrimary,
                )
                Spacer(modifier = Modifier.height(SeyirSpacing.Item))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(SeyirSpacing.Item),
                ) {
                    AppearanceTimeField(
                        value = startText,
                        onValueChange = { startText = it.take(5) },
                        label = "Başlangıç",
                    )
                    AppearanceTimeField(
                        value = endText,
                        onValueChange = { endText = it.take(5) },
                        label = "Bitiş",
                    )
                    TvAction(
                        text = "Saatleri kaydet",
                        enabled = timesValid,
                        onClick = {
                            onScheduleChanged(
                                scheduleEnabled,
                                parsedStart ?: startMinutes,
                                parsedEnd ?: endMinutes,
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp),
                        unfocusedFontWeight = FontWeight.Medium,
                    )
                }

                Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
                Text(
                    text = if (timesValid) {
                        "Örnek: 20:00 → 07:00. Gece yarısını aşan aralıklar desteklenir."
                    } else {
                        "Saati 24 saat biçiminde HH:mm olarak girin."
                    },
                    fontSize = SeyirType.Meta,
                    color = if (timesValid) SeyirColors.TextTertiary else SeyirColors.TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(SeyirSpacing.Section))
            Text(
                text = "Koyu tema saf siyah kullanmaz; Google TV tarzı koyu gri yüzeyler kullanır.",
                fontSize = SeyirType.Meta,
                color = SeyirColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun AppearanceTimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.width(190.dp),
        singleLine = true,
        label = { MaterialText(label) },
        placeholder = { MaterialText("20:00") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SeyirColors.TextPrimary,
            unfocusedTextColor = SeyirColors.TextPrimary,
            focusedBorderColor = SeyirColors.Accent,
            unfocusedBorderColor = SeyirColors.Hairline,
            focusedLabelColor = SeyirColors.Accent,
            unfocusedLabelColor = SeyirColors.TextSecondary,
            cursorColor = SeyirColors.Accent,
        ),
    )
}
