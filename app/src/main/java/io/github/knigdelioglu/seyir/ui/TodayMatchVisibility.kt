package io.github.knigdelioglu.seyir.ui

import io.github.knigdelioglu.seyir.data.TodayMatch

internal const val DEFAULT_MATCH_ACTIVE_WINDOW_SECONDS = 3 * 60 * 60L

private val NON_PLAYABLE_STATUS_CODES = setOf(
    "PST",
    "CANC",
    "ABD",
    "AWD",
    "WO",
)

/**
 * Keeps only matches that are relevant at the current moment.
 *
 * Seyir intentionally avoids polling the football API throughout the day. Because the cached
 * morning response can still say NS after kickoff, a non-finished match remains visible for a
 * bounded window after kickoff so an in-progress match does not disappear merely because the
 * cache is stale. A manual refresh that reports FT/AET/PEN removes it immediately.
 */
internal fun selectCurrentAndUpcomingMatches(
    matches: List<TodayMatch>,
    nowEpochSeconds: Long,
    activeWindowSeconds: Long = DEFAULT_MATCH_ACTIVE_WINDOW_SECONDS,
): List<TodayMatch> = matches.filter { match ->
    when {
        match.isFinished -> false
        match.statusShort in NON_PLAYABLE_STATUS_CODES -> false
        match.isLive -> true
        match.kickoffEpochSeconds >= nowEpochSeconds -> true
        else -> nowEpochSeconds - match.kickoffEpochSeconds <= activeWindowSeconds
    }
}
