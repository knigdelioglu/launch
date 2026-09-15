package io.github.knigdelioglu.seyir.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


data class TodayMatch(
    val fixtureId: Long,
    val leagueName: String,
    val countryName: String,
    val homeTeam: String,
    val awayTeam: String,
    val kickoffEpochSeconds: Long,
    val statusShort: String,
    val statusLong: String,
    val elapsedMinute: Int?,
    val homeGoals: Int?,
    val awayGoals: Int?,
) {
    val isLive: Boolean
        get() = statusShort in LIVE_STATUS_CODES

    val isFinished: Boolean
        get() = statusShort in FINISHED_STATUS_CODES

    private companion object {
        val LIVE_STATUS_CODES = setOf("1H", "HT", "2H", "ET", "BT", "P", "INT", "LIVE")
        val FINISHED_STATUS_CODES = setOf("FT", "AET", "PEN")
    }
}

class TodayMatchRepository {
    suspend fun loadTodayMatches(
        apiKey: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
        date: LocalDate = LocalDate.now(zoneId),
    ): List<TodayMatch> = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "API-Football anahtarı ayarlanmamış." }

        val dateValue = date.toString()
        val timezoneValue = URLEncoder.encode(zoneId.id, StandardCharsets.UTF_8.name())
        val url = URL(
            "$BASE_URL/fixtures?date=$dateValue&timezone=$timezoneValue",
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("x-apisports-key", apiKey.trim())
        }

        try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299) {
                throw IllegalStateException(
                    "Maç verisi alınamadı (HTTP $responseCode).",
                )
            }

            parseResponse(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResponse(body: String): List<TodayMatch> {
        val root = JSONObject(body)
        val errors = root.opt("errors")
        if (errors is JSONObject && errors.length() > 0) {
            val message = errors.keys().asSequence()
                .mapNotNull { key -> errors.optString(key).takeIf(String::isNotBlank) }
                .joinToString(" • ")
                .ifBlank { "API-Football isteği reddedildi." }
            throw IllegalStateException(message)
        }

        val response = root.optJSONArray("response") ?: return emptyList()
        val matches = buildList {
            for (index in 0 until response.length()) {
                val item = response.optJSONObject(index) ?: continue
                val fixture = item.optJSONObject("fixture") ?: continue
                val league = item.optJSONObject("league") ?: continue
                val teams = item.optJSONObject("teams") ?: continue
                val goals = item.optJSONObject("goals")
                val status = fixture.optJSONObject("status")
                val home = teams.optJSONObject("home")
                val away = teams.optJSONObject("away")

                val fixtureId = fixture.optLong("id", -1L)
                val kickoff = fixture.optLong("timestamp", -1L)
                val homeName = home?.optString("name").orEmpty()
                val awayName = away?.optString("name").orEmpty()
                if (fixtureId <= 0L || kickoff <= 0L || homeName.isBlank() || awayName.isBlank()) {
                    continue
                }

                add(
                    TodayMatch(
                        fixtureId = fixtureId,
                        leagueName = league.optString("name").orEmpty(),
                        countryName = league.optString("country").orEmpty(),
                        homeTeam = homeName,
                        awayTeam = awayName,
                        kickoffEpochSeconds = kickoff,
                        statusShort = status?.optString("short").orEmpty(),
                        statusLong = status?.optString("long").orEmpty(),
                        elapsedMinute = status?.nullableInt("elapsed"),
                        homeGoals = goals?.nullableInt("home"),
                        awayGoals = goals?.nullableInt("away"),
                    ),
                )
            }
        }

        return matches
            .sortedWith(
                compareBy<TodayMatch>(::competitionPriority)
                    .thenBy { it.kickoffEpochSeconds },
            )
            .take(MAX_HOME_MATCHES)
    }

    private fun competitionPriority(match: TodayMatch): Int {
        val league = match.leagueName.lowercase()
        val country = match.countryName.lowercase()

        return when {
            "süper lig" in league ||
                "super lig" in league ||
                "türkiye kupası" in league ||
                "turkiye kupasi" in league ||
                "champions league" in league ||
                "europa league" in league ||
                "conference league" in league -> 0

            country == "turkey" || country == "türkiye" -> 1

            league in TOP_EUROPEAN_LEAGUES -> 2
            else -> 3
        }
    }

    private fun JSONObject.nullableInt(key: String): Int? =
        if (isNull(key)) null else optInt(key)

    companion object {
        private const val BASE_URL = "https://v3.football.api-sports.io"
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val MAX_HOME_MATCHES = 12

        private val TOP_EUROPEAN_LEAGUES = setOf(
            "premier league",
            "la liga",
            "serie a",
            "bundesliga",
            "ligue 1",
        )

        fun kickoffInstant(match: TodayMatch): Instant =
            Instant.ofEpochSecond(match.kickoffEpochSeconds)
    }
}
