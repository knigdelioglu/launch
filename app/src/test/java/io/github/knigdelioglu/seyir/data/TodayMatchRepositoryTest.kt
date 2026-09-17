package io.github.knigdelioglu.seyir.data

import io.github.knigdelioglu.seyir.ui.formatMatchKickoffTime
import io.github.knigdelioglu.seyir.ui.formatMatchScheduleText
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayMatchRepositoryTest {
    private val repository = TodayMatchRepository()

    @Test
    fun selectMatchesForHome_keepsOnlyFeaturedTeams() {
        val matches = listOf(
            match(fixtureId = 1, kickoffEpochSeconds = 3_000, homeTeamId = 34, homeTeam = "Newcastle United"),
            match(fixtureId = 2, kickoffEpochSeconds = 2_000, homeTeamId = 645, homeTeam = "Galatasaray"),
            match(fixtureId = 3, kickoffEpochSeconds = 1_000, homeTeamId = 42, homeTeam = "Arsenal"),
            match(fixtureId = 4, kickoffEpochSeconds = 4_000, homeTeamId = 157, homeTeam = "Bayern Munich"),
        )

        val selected = repository.selectMatchesForHome(matches)

        assertEquals(listOf(3L, 2L), selected.map { it.fixtureId })
    }

    @Test
    fun selectMatchesForHome_usesTeamNameWhenApiIdIsMissing() {
        val selected = repository.selectMatchesForHome(
            listOf(
                match(fixtureId = 1, kickoffEpochSeconds = 1_000, homeTeam = "Beşiktaş", homeTeamId = 0),
                match(fixtureId = 2, kickoffEpochSeconds = 2_000, homeTeam = "Trabzonspor", homeTeamId = 0),
            ),
        )

        assertEquals(listOf(1L), selected.map { it.fixtureId })
    }

    @Test
    fun selectMatchesForHome_keepsChronologicalScheduleEvenWithFavorites() {
        val selected = repository.selectMatchesForHome(
            listOf(
                match(fixtureId = 1, kickoffEpochSeconds = 2_000, homeTeamId = 645, homeTeam = "Galatasaray"),
                match(fixtureId = 2, kickoffEpochSeconds = 1_000, homeTeamId = 42, homeTeam = "Arsenal"),
            ),
            favoriteTeamNames = setOf("Galatasaray"),
        )

        assertEquals(listOf(2L, 1L), selected.map { it.fixtureId })
    }

    @Test
    fun turkeyTimeZone_isUsedForKickoffConversion() {
        val time = Instant
            .parse("2026-09-17T15:00:00Z")
            .atZone(TodayMatchRepository.TURKEY_TIME_ZONE)
        val kickoffEpochSeconds = time.toEpochSecond()

        assertEquals("Europe/Istanbul", TodayMatchRepository.TURKEY_TIME_ZONE.id)
        assertEquals("18:00", formatMatchKickoffTime(kickoffEpochSeconds))
        assertEquals("18:00", formatMatchScheduleText(match(1, kickoffEpochSeconds, 42, "Arsenal", "2H")))
        assertEquals("18:00", formatMatchScheduleText(match(2, kickoffEpochSeconds, 42, "Arsenal", "FT")))
    }

    private fun match(
        fixtureId: Long,
        kickoffEpochSeconds: Long,
        homeTeamId: Int,
        homeTeam: String,
        statusShort: String = "NS",
    ) = TodayMatch(
        fixtureId = fixtureId,
        leagueName = "Test League",
        countryName = "Test Country",
        homeTeamId = homeTeamId,
        homeTeam = homeTeam,
        awayTeamId = 999,
        awayTeam = "Test Away",
        kickoffEpochSeconds = kickoffEpochSeconds,
        statusShort = statusShort,
        statusLong = "Not Started",
        elapsedMinute = null,
        homeGoals = null,
        awayGoals = null,
    )
}
