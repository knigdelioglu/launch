package io.github.knigdelioglu.seyir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.knigdelioglu.seyir.data.FavoriteTeam
import io.github.knigdelioglu.seyir.ui.theme.SeyirColors
import io.github.knigdelioglu.seyir.ui.theme.SeyirRadius
import io.github.knigdelioglu.seyir.ui.theme.SeyirSpacing
import io.github.knigdelioglu.seyir.ui.theme.SeyirType

@Composable
fun FavoriteTeamsScreen(
    selectedTeams: List<FavoriteTeam>,
    searchResults: List<FavoriteTeam>,
    searchLoading: Boolean,
    searchError: String?,
    onSearch: (String) -> Unit,
    onToggleTeam: (FavoriteTeam) -> Unit,
    onClearSearch: () -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val selectedIds = remember(selectedTeams) { selectedTeams.mapTo(hashSetOf()) { it.id } }

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
                text = "‹  Bugün ne var",
                onClick = onBack,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Section))

            Text(
                text = "Takımlarım",
                fontSize = SeyirType.Hero,
                fontWeight = FontWeight.SemiBold,
                color = SeyirColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Tiny))
            Text(
                text = "Takım adları cihazda saklanır; kart yalnızca öne çıkan takımların maçlarını gösterir.",
                fontSize = SeyirType.Subtitle,
                color = SeyirColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(SeyirSpacing.Section))
            Text(
                text = "Seçili takımlar",
                fontSize = SeyirType.SectionTitle,
                fontWeight = FontWeight.SemiBold,
                color = SeyirColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Compact))

            if (selectedTeams.isEmpty()) {
                Text(
                    text = "Henüz takım seçmediniz.",
                    fontSize = SeyirType.Meta,
                    color = SeyirColors.TextTertiary,
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(SeyirSpacing.Compact),
                ) {
                    items(
                        items = selectedTeams,
                        key = { it.id },
                    ) { team ->
                        SelectedTeamCard(
                            team = team,
                            onClick = { onToggleTeam(team) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SeyirSpacing.Section))
            Text(
                text = "Takım ekle",
                fontSize = SeyirType.SectionTitle,
                fontWeight = FontWeight.SemiBold,
                color = SeyirColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(SeyirSpacing.Compact))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SeyirSpacing.Item),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        if (it.isBlank()) onClearSearch()
                    },
                    modifier = Modifier.width(620.dp),
                    singleLine = true,
                    label = { MaterialText("Takım adı") },
                    placeholder = { MaterialText("Örn. Fenerbahçe") },
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
                TeamAction(
                    text = "Ekle",
                    enabled = query.trim().length >= 3 && !searchLoading,
                    onClick = { onSearch(query) },
                )
            }

            Spacer(modifier = Modifier.height(SeyirSpacing.Item))

            when {
                searchError != null -> Text(
                    text = searchError,
                    fontSize = SeyirType.Meta,
                    color = SeyirColors.TextSecondary,
                )

                searchResults.isNotEmpty() -> {
                    Text(
                        text = "Onayla",
                        fontSize = SeyirType.Meta,
                        fontWeight = FontWeight.Medium,
                        color = SeyirColors.TextTertiary,
                    )
                    Spacer(modifier = Modifier.height(SeyirSpacing.Compact))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(SeyirSpacing.Item),
                        verticalArrangement = Arrangement.spacedBy(SeyirSpacing.Compact),
                    ) {
                        items(
                            items = searchResults,
                            key = { it.id },
                        ) { team ->
                            TeamResultCard(
                                team = team,
                                selected = team.id in selectedIds,
                                onClick = { onToggleTeam(team) },
                            )
                        }
                    }
                }

                query.trim().length >= 3 -> Text(
                    text = "Takım adını hazırlamak için Ekle düğmesine basın.",
                    fontSize = SeyirType.Meta,
                    color = SeyirColors.TextTertiary,
                )
            }
        }
    }
}

@Composable
private fun SelectedTeamCard(
    team: FavoriteTeam,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .width(210.dp)
            .tvFocusScale(focused, "selected-team-scale")
            .clip(RoundedCornerShape(SeyirRadius.Action))
            .background(
                if (focused) SeyirColors.SurfaceFocused else SeyirColors.SurfaceElevated,
            )
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick(onClick = onClick)
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "★  ${team.name}",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = SeyirType.CardLabel,
            fontWeight = FontWeight.SemiBold,
            color = if (focused) SeyirColors.Accent else SeyirColors.TextPrimary,
        )
        if (team.country.isNotBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = team.country,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = SeyirType.Meta,
                color = SeyirColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun TeamResultCard(
    team: FavoriteTeam,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .tvFocusScale(focused, "team-result-scale")
            .clip(RoundedCornerShape(SeyirRadius.Action))
            .background(
                if (focused) SeyirColors.SurfaceFocused else SeyirColors.SurfaceSoft,
            )
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick(onClick = onClick)
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = team.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = SeyirType.CardLabel,
                fontWeight = if (focused || selected) FontWeight.SemiBold else FontWeight.Medium,
                color = SeyirColors.TextPrimary,
            )
        }
        Text(
            text = if (selected) "✓" else "+",
            fontSize = SeyirType.SectionTitle,
            fontWeight = FontWeight.SemiBold,
            color = if (selected || focused) SeyirColors.Accent else SeyirColors.TextSecondary,
        )
    }
}

@Composable
private fun TeamAction(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TvAction(
        text = text,
        enabled = enabled,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp),
        unfocusedFontWeight = FontWeight.Medium,
    )
}
