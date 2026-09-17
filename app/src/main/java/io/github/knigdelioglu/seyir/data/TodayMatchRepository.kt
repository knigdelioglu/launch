package io.github.knigdelioglu.seyir.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale


data class TodayMatch(
    val fixtureId: Long,
    val leagueName: String,
    val countryName: String,
    val homeTeamId: Int = 0,
    val homeTeam: String,
    val awayTeamId: Int = 0,
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

data class DailyMatchResult(
    val rawJson: String,
    val matches: List<TodayMatch>,
)

class TodayMatchRepository {
    suspend fun loadTodayMatches(
        apiKey: String,
        zoneId: ZoneId = TURKEY_TIME_ZONE,
        date: LocalDate = LocalDate.now(TURKEY_TIME_ZONE),
        favoriteTeamNames: Set<String> = emptySet(),
    ): DailyMatchResult = withContext(Dispatchers.IO) {
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
                val rawMessage = runCatching {
                    val root = JSONObject(body)
                    root.optJSONObject("errors")?.let { errors ->
                        errors.keys().asSequence()
                            .mapNotNull { key -> errors.optString(key).takeIf(String::isNotBlank) }
                            .joinToString(" • ")
                    }
                }.getOrNull()
                val message = when {
                    responseCode == 429 -> "API-Football günlük 100 istek kotanız doldu."
                    responseCode == 401 || responseCode == 403 -> "API-Football anahtarı geçersiz."
                    else -> rawMessage ?: "Maç verisi alınamadı (HTTP $responseCode)."
                }
                throw IllegalStateException(message)
            }

            val matches = parseResponse(
                body = body,
                favoriteTeamNames = favoriteTeamNames,
            )
            DailyMatchResult(rawJson = body, matches = matches)
        } finally {
            connection.disconnect()
        }
    }

    fun parseCachedMatches(
        rawJson: String,
        date: LocalDate = LocalDate.now(TURKEY_TIME_ZONE),
        zoneId: ZoneId = TURKEY_TIME_ZONE,
        favoriteTeamNames: Set<String> = emptySet(),
    ): List<TodayMatch> {
        if (rawJson.isBlank()) return emptyList()
        return runCatching {
            parseResponse(rawJson, favoriteTeamNames)
        }.getOrDefault(emptyList())
    }

    private fun parseResponse(
        body: String,
        favoriteTeamNames: Set<String>,
    ): List<TodayMatch> {
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
                        homeTeamId = home?.optInt("id", 0) ?: 0,
                        homeTeam = homeName,
                        awayTeamId = away?.optInt("id", 0) ?: 0,
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

        return selectMatchesForHome(
            matches = matches,
            favoriteTeamNames = favoriteTeamNames,
        )
    }

    internal fun selectMatchesForHome(
        matches: List<TodayMatch>,
        favoriteTeamNames: Set<String> = emptySet(),
    ): List<TodayMatch> {
        val normalizedFavorites = favoriteTeamNames
            .asSequence()
            .map(::normalizeName)
            .filter(String::isNotBlank)
            .toSet()

        return matches
            .filter { it.isFeaturedMatch() }
            .distinctBy { it.fixtureId }
            .sortedWith(
                compareBy<TodayMatch> { it.kickoffEpochSeconds }
                    .thenBy { if (it.involvesFavorite(normalizedFavorites)) 0 else 1 }
                    .thenBy { it.fixtureId },
            )
            .take(MAX_HOME_MATCHES)
    }

    private fun TodayMatch.isFeaturedMatch(): Boolean =
        homeTeamId in FEATURED_TEAM_IDS || awayTeamId in FEATURED_TEAM_IDS ||
            normalizeName(homeTeam) in FEATURED_TEAM_NAMES ||
            normalizeName(awayTeam) in FEATURED_TEAM_NAMES

    private fun TodayMatch.involvesFavorite(normalizedFavorites: Set<String>): Boolean {
        if (normalizedFavorites.isEmpty()) return false
        val home = normalizeName(homeTeam)
        val away = normalizeName(awayTeam)
        return normalizedFavorites.any { favorite ->
            favorite == home || favorite == away ||
                home.contains(favorite) || away.contains(favorite) ||
                favorite.contains(home) || favorite.contains(away)
        }
    }

    private fun normalizeName(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase(Locale.ROOT)
        .replace('ı', 'i')
        .replace("[^a-z0-9 ]".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
        .trim()

    private fun JSONObject.nullableInt(key: String): Int? =
        if (!has(key) || isNull(key)) null else optInt(key)

    companion object {
        val TURKEY_TIME_ZONE: ZoneId = ZoneId.of("Europe/Istanbul")

        private const val BASE_URL = "https://v3.football.api-sports.io"
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val MAX_HOME_MATCHES = 12

        // Premier League Big Six plus the three Turkish clubs and the two Spanish clubs.
        private val FEATURED_TEAM_IDS = setOf(
            33,  // Manchester United
            40,  // Liverpool
            42,  // Arsenal
            47,  // Tottenham Hotspur
            49,  // Chelsea
            50,  // Manchester City
            529, // Barcelona
            541, // Real Madrid
            549, // Beşiktaş
            611, // Fenerbahçe
            645, // Galatasaray
        )

        private val FEATURED_TEAM_NAMES = setOf(
            "arsenal",
            "chelsea",
            "liverpool",
            "manchester city",
            "manchester united",
            "manchester utd",
            "man utd",
            "tottenham",
            "tottenham hotspur",
            "barcelona",
            "real madrid",
            "besiktas",
            "fenerbahce",
            "galatasaray",
        )
    }
}
