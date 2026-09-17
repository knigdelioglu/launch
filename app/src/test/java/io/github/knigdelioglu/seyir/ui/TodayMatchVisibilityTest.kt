package io.github.knigdelioglu.seyir.ui

import io.github.knigdelioglu.seyir.data.TodayMatch
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayMatchVisibilityTest {
    @Test
    fun keepsLiveAndFutureMatches_butDropsFinishedAndOldMatches() {
        val now = 10_000L
        val matches = listOf(
            match(id = 1, kickoff = now - 600, status = "1H"),
            match(id = 2, kickoff = now + 3_600, status = "NS"),
            match(id = 3, kickoff = now - 600, status = "FT"),
            match(id = 4, kickoff = now - DEFAULT_MATCH_ACTIVE_WINDOW_SECONDS - 1, status = "NS"),
        )

        val visible = selectCurrentAndUpcomingMatches(matches, now)

        assertEquals(listOf(1L, 2L), visible.map { it.fixtureId })
    }

    @Test
    fun staleScheduledMatch_staysVisibleInsideActiveWindow() {
        val now = 20_000L
        val match = match(
            id = 1,
            kickoff = now - 60 * 60,
            status = "NS",
        )

        assertEquals(
            listOf(1L),
            selectCurrentAndUpcomingMatches(listOf(match), now).map { it.fixtureId },
        )
    }

    @Test
    fun postponedCancelledAndAbandonedMatches_areHidden() {
        val now = 30_000L
        val matches = listOf(
            match(id = 1, kickoff = now + 1_000, status = "PST"),
            match(id = 2, kickoff = now + 1_000, status = "CANC"),
            match(id = 3, kickoff = now + 1_000, status = "ABD"),
            match(id = 4, kickoff = now + 1_000, status = "AWD"),
            match(id = 5, kickoff = now + 1_000, status = "WO"),
        )

        assertEquals(emptyList<Long>(), selectCurrentAndUpcomingMatches(matches, now).map { it.fixtureId })
    }

    private fun match(
        id: Long,
        kickoff: Long,
        status: String,
    ) = TodayMatch(
        fixtureId = id,
        leagueName = "Test League",
        countryName = "Test Country",
        homeTeamId = 42,
        homeTeam = "Arsenal",
        awayTeamId = 999,
        awayTeam = "Opponent",
        kickoffEpochSeconds = kickoff,
        statusShort = status,
        statusLong = status,
        elapsedMinute = null,
        homeGoals = null,
        awayGoals = null,
    )
}
