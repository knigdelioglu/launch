package io.github.knigdelioglu.seyir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.knigdelioglu.seyir.ui.theme.SeyirColors
import io.github.knigdelioglu.seyir.ui.theme.SeyirRadius
import io.github.knigdelioglu.seyir.ui.theme.SeyirSpacing
import io.github.knigdelioglu.seyir.ui.theme.SeyirType

@Composable
fun SportsSettingsScreen(
    configured: Boolean,
    favoriteTeamCount: Int,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
    onOpenFavoriteTeams: () -> Unit,
    onBack: () -> Unit,
) {
    var apiKey by remember { mutableStateOf("") }

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
            SportsBackAction(onClick = onBack)
            Spacer(modifier = Modifier.height(SeyirSpacing.Section))

            Text(
                text = "Bugün ne var",
                fontSize = SeyirType.Hero,
                fontWeight = FontWeight.SemiBold,
                color = SeyirColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
            Text(
                text = "Günün öne çıkan futbol maçlarını Gemini + Google Search ile bir kez bulur.",
                fontSize = SeyirType.Subtitle,
                color = SeyirColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(SeyirSpacing.SectionLarge))

            Text(
                text = "Veri kaynağı",
                fontSize = SeyirType.SectionTitle,
                fontWeight = FontWeight.SemiBold,
                color = SeyirColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Item))

            Column(
                modifier = Modifier
                    .width(720.dp)
                    .clip(RoundedCornerShape(SeyirRadius.Card))
                    .background(SeyirColors.SurfaceSoft)
                    .padding(22.dp),
            ) {
                Text(
                    text = "Gemini API + Google Search",
                    fontSize = SeyirType.SectionTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = SeyirColors.TextPrimary,
                )
                Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
                Text(
                    text = if (configured) {
                        "Bağlı • anahtar yalnız bu cihazda saklanıyor"
                    } else {
                        "Bağlı değil • Gemini API anahtarı gerekiyor"
                    },
                    fontSize = SeyirType.Meta,
                    color = if (configured) SeyirColors.Accent else SeyirColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(SeyirSpacing.Item))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    label = { MaterialText("Gemini API anahtarı") },
                    placeholder = { MaterialText("AIza…") },
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

                Spacer(modifier = Modifier.height(SeyirSpacing.Item))
                Row {
                    SportsAction(
                        text = "Anahtarı kaydet",
                        enabled = apiKey.isNotBlank(),
                        onClick = {
                            onSaveKey(apiKey)
                            apiKey = ""
                        },
                    )
                    if (configured) {
                        Spacer(modifier = Modifier.width(SeyirSpacing.Item))
                        SportsAction(
                            text = "Bağlantıyı kaldır",
                            enabled = true,
                            onClick = onClearKey,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(SeyirSpacing.Item))
                SportsAction(
                    text = if (favoriteTeamCount == 0) {
                        "Takımlarım • takım ekle"
                    } else {
                        "Takımlarım • $favoriteTeamCount seçili"
                    },
                    enabled = true,
                    onClick = onOpenFavoriteTeams,
                )
            }

            Spacer(modifier = Modifier.height(SeyirSpacing.Section))
            Text(
                text = "Seyir bir takvim gününde en fazla 1 otomatik Gemini isteği yapar. Sonuç tarih, veri ve alınma zamanı ile cihazda kalıcı saklanır; uygulamayı veya TV'yi yeniden açmak aynı gün yeni istek oluşturmaz. Takımlarım değişiklikleri mevcut günlük listede eşleşiyorsa anında yeniden sıralanır, yeni Gemini sorgusu ertesi gün yapılır.",
                fontSize = SeyirType.Meta,
                color = SeyirColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun SportsAction(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(SeyirRadius.Action))
            .background(
                when {
                    !enabled -> SeyirColors.SurfaceSoft.copy(alpha = 0.45f)
                    focused -> SeyirColors.SurfaceFocused
                    else -> SeyirColors.SurfaceSoft
                },
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            fontSize = SeyirType.CardLabel,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
            color = when {
                !enabled -> SeyirColors.TextTertiary
                focused -> SeyirColors.TextPrimary
                else -> SeyirColors.TextSecondary
            },
        )
    }
}

@Composable
private fun SportsBackAction(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }

    Text(
        text = "‹  Ayarlar",
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
