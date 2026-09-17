package io.github.knigdelioglu.seyir.data

import io.github.knigdelioglu.seyir.ui.formatMatchKickoffTime
import io.github.knigdelioglu.seyir.ui.formatMatchScheduleText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

    @Test
    fun parseCachedMatches_ignoresMalformedStoredJson() {
        assertTrue(repository.parseCachedMatches("not-json").isEmpty())
    }

    @Test
    fun cachedFavoriteTeam_changesSelectionOrder_withoutAnotherNetworkCall() {
        val matches = repository.parseCachedMatches(
            rawJson = twoFixturesJson(),
            favoriteTeamNames = setOf("Galatasaray"),
        )

        assertEquals(listOf(2L, 1L), matches.map { it.fixtureId })
    }

    @Test
    fun loadTodayMatches_usesInjectableClient_andParsesResponse() = runBlocking {
        val client = RecordingFootballApiClient(fixturesJson())
        val repository = TodayMatchRepository(client)

        val result = repository.loadTodayMatches(
            apiKey = "test-key",
            zoneId = ZoneId.of("Europe/Istanbul"),
            date = LocalDate.parse("2026-09-17"),
        )

        assertEquals("test-key", client.apiKey)
        assertEquals(LocalDate.parse("2026-09-17"), client.date)
        assertEquals("Europe/Istanbul", client.zoneId?.id)
        assertNotNull(result.rawJson)
        assertEquals(listOf(1L), result.matches.map { it.fixtureId })
    }

    @Test
    fun loadTodayMatches_preservesTypedApiErrors() = runBlocking {
        val expected = FootballApiException(
            kind = FootballApiException.Kind.RATE_LIMITED,
            message = "rate limited",
        )
        val repository = TodayMatchRepository(FailingFootballApiClient(expected))

        try {
            repository.loadTodayMatches(
                apiKey = "test-key",
                zoneId = TodayMatchRepository.TURKEY_TIME_ZONE,
                date = LocalDate.parse("2026-09-17"),
            )
        } catch (actual: FootballApiException) {
            assertEquals(FootballApiException.Kind.RATE_LIMITED, actual.kind)
            return@runBlocking
        }
        throw AssertionError("Expected FootballApiException")
    }

    private fun fixturesJson(
        fixtureId: Long = 1,
        homeId: Int = 42,
        homeName: String = "Arsenal",
        awayId: Int = 999,
        awayName: String = "Test Away",
    ): String = """
        {
          "response": [
            {
              "fixture": {"id": $fixtureId, "timestamp": 2000},
              "league": {"name": "Test League", "country": "Test Country"},
              "teams": {
                "home": {"id": $homeId, "name": "$homeName"},
                "away": {"id": $awayId, "name": "$awayName"}
              },
              "goals": {"home": null, "away": null},
              "status": {"short": "NS", "long": "Not Started", "elapsed": null}
            }
          ]
        }
    """.trimIndent()

    private fun twoFixturesJson(): String = """
        {
          "response": [
            {
              "fixture": {"id": 1, "timestamp": 2000},
              "league": {"name": "Test League", "country": "Test Country"},
              "teams": {
                "home": {"id": 42, "name": "Arsenal"},
                "away": {"id": 999, "name": "Test Away"}
              },
              "goals": {"home": null, "away": null},
              "status": {"short": "NS", "long": "Not Started", "elapsed": null}
            },
            {
              "fixture": {"id": 2, "timestamp": 2000},
              "league": {"name": "Test League", "country": "Test Country"},
              "teams": {
                "home": {"id": 645, "name": "Galatasaray"},
                "away": {"id": 999, "name": "Test Away"}
              },
              "goals": {"home": null, "away": null},
              "status": {"short": "NS", "long": "Not Started", "elapsed": null}
            }
          ]
        }
    """.trimIndent()

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

private class RecordingFootballApiClient(
    private val response: String,
) : FootballApiClient {
    var apiKey: String? = null
    var date: LocalDate? = null
    var zoneId: ZoneId? = null

    override suspend fun fetchFixtures(apiKey: String, date: LocalDate, zoneId: ZoneId): String {
        this.apiKey = apiKey
        this.date = date
        this.zoneId = zoneId
        return response
    }
}

private class FailingFootballApiClient(
    private val error: FootballApiException,
) : FootballApiClient {
    override suspend fun fetchFixtures(apiKey: String, date: LocalDate, zoneId: ZoneId): String {
        throw error
    }
}
