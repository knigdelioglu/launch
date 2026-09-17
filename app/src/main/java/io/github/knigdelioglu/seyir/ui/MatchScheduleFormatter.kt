package io.github.knigdelioglu.seyir.ui

import io.github.knigdelioglu.seyir.data.TodayMatch
import io.github.knigdelioglu.seyir.data.TodayMatchRepository
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun formatMatchKickoffTime(kickoffEpochSeconds: Long): String =
    Instant.ofEpochSecond(kickoffEpochSeconds)
        .atZone(TodayMatchRepository.TURKEY_TIME_ZONE)
        .format(MATCH_TIME_FORMATTER)

internal fun formatMatchScheduleText(match: TodayMatch): String = when (match.statusShort) {
    "PST" -> "Ertelendi"
    "CANC" -> "İptal"
    else -> formatMatchKickoffTime(match.kickoffEpochSeconds)
}

private val MATCH_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
