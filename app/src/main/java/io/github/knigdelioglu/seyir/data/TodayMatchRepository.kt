package io.github.knigdelioglu.seyir.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
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

class FootballApiException(
    val kind: Kind,
    val statusCode: Int? = null,
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause) {
    enum class Kind {
        UNAUTHORIZED,
        RATE_LIMITED,
        NETWORK,
        INVALID_RESPONSE,
        UNKNOWN,
    }
}

interface FootballApiClient {
    suspend fun fetchFixtures(
        apiKey: String,
        date: LocalDate,
        zoneId: ZoneId,
    ): String
}

class HttpFootballApiClient(
    private val connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    },
) : FootballApiClient {
    override suspend fun fetchFixtures(
        apiKey: String,
        date: LocalDate,
        zoneId: ZoneId,
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw FootballApiException(
                kind = FootballApiException.Kind.UNAUTHORIZED,
                message = "API-Football anahtarı ayarlanmamış.",
            )
        }

        val timezoneValue = URLEncoder.encode(zoneId.id, StandardCharsets.UTF_8.name())
        val url = URL(
            "$BASE_URL/fixtures?date=$date&timezone=$timezoneValue",
        )
        val connection = try {
            connectionFactory(url).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("x-apisports-key", apiKey.trim())
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            throw FootballApiException(
                kind = FootballApiException.Kind.NETWORK,
                message = "API-Football bağlantısı kurulamadı.",
                cause = error,
            )
        } catch (error: Exception) {
            throw FootballApiException(
                kind = FootballApiException.Kind.UNKNOWN,
                message = "API-Football bağlantısı başlatılamadı.",
                cause = error,
            )
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
                throw httpError(responseCode, body)
            }
            if (body.isBlank()) {
                throw FootballApiException(
                    kind = FootballApiException.Kind.INVALID_RESPONSE,
                    statusCode = responseCode,
                    message = "API-Football boş yanıt döndürdü.",
                )
            }
            body
        } catch (error: CancellationException) {
            throw error
        } catch (error: FootballApiException) {
            throw error
        } catch (error: IOException) {
            throw FootballApiException(
                kind = FootballApiException.Kind.NETWORK,
                message = "API-Football yanıtı alınamadı.",
                cause = error,
            )
        } catch (error: Exception) {
            throw FootballApiException(
                kind = FootballApiException.Kind.UNKNOWN,
                message = "API-Football isteği tamamlanamadı.",
                cause = error,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun httpError(responseCode: Int, body: String): FootballApiException {
        val serverMessage = try {
            JSONObject(body).optJSONObject("errors")?.let { errors ->
                errors.keys().asSequence()
                    .mapNotNull { key -> errors.optString(key).takeIf(String::isNotBlank) }
                    .joinToString(" • ")
            }
        } catch (_: JSONException) {
            null
        }

        val kind = when (responseCode) {
            401, 403 -> FootballApiException.Kind.UNAUTHORIZED
            429 -> FootballApiException.Kind.RATE_LIMITED
            in 500..599 -> FootballApiException.Kind.NETWORK
            else -> FootballApiException.Kind.UNKNOWN
        }
        return FootballApiException(
            kind = kind,
            statusCode = responseCode,
            message = serverMessage ?: "API-Football HTTP $responseCode yanıtı döndürdü.",
        )
    }

    private companion object {
        const val BASE_URL = "https://v3.football.api-sports.io"
        const val CONNECT_TIMEOUT_MS = 8_000
        const val READ_TIMEOUT_MS = 10_000
    }
}

class TodayMatchRepository(
    private val apiClient: FootballApiClient = HttpFootballApiClient(),
) : TodayMatchSource {
    private val cachedParseMutex = Mutex()
    private var cachedRawJson: String? = null
    private var cachedFavoriteTeamNames: Set<String> = emptySet()
    private var cachedMatches: List<TodayMatch> = emptyList()

    override suspend fun loadTodayMatches(
        apiKey: String,
        zoneId: ZoneId,
        date: LocalDate,
        favoriteTeamNames: Set<String>,
    ): DailyMatchResult {
        val body = apiClient.fetchFixtures(
            apiKey = apiKey,
            date = date,
            zoneId = zoneId,
        )
        val normalizedFavoriteTeamNames = favoriteTeamNames.toSet()
        val matches = try {
            parseMatchesOffMain(body, normalizedFavoriteTeamNames)
        } catch (error: CancellationException) {
            throw error
        } catch (error: FootballApiException) {
            throw error
        } catch (error: JSONException) {
            throw FootballApiException(
                kind = FootballApiException.Kind.INVALID_RESPONSE,
                message = "API-Football geçersiz veri döndürdü.",
                cause = error,
            )
        }
        rememberCachedParse(
            rawJson = body,
            favoriteTeamNames = normalizedFavoriteTeamNames,
            matches = matches,
        )
        return DailyMatchResult(rawJson = body, matches = matches)
    }

    override suspend fun parseCachedMatches(
        rawJson: String,
        favoriteTeamNames: Set<String>,
    ): List<TodayMatch> = cachedParseMutex.withLock {
        val normalizedFavoriteTeamNames = favoriteTeamNames.toSet()
        if (
            rawJson == cachedRawJson &&
            normalizedFavoriteTeamNames == cachedFavoriteTeamNames
        ) {
            return@withLock cachedMatches
        }

        val matches = if (rawJson.isBlank()) {
            emptyList()
        } else {
            try {
                parseMatchesOffMain(rawJson, normalizedFavoriteTeamNames)
            } catch (error: CancellationException) {
                throw error
            } catch (_: FootballApiException) {
                emptyList()
            } catch (_: JSONException) {
                emptyList()
            }
        }

        cachedRawJson = rawJson
        cachedFavoriteTeamNames = normalizedFavoriteTeamNames
        cachedMatches = matches
        matches
    }

    private suspend fun parseMatchesOffMain(
        rawJson: String,
        favoriteTeamNames: Set<String>,
    ): List<TodayMatch> = withContext(Dispatchers.Default) {
        FootballMatchParser.parse(rawJson, favoriteTeamNames)
    }

    private suspend fun rememberCachedParse(
        rawJson: String,
        favoriteTeamNames: Set<String>,
        matches: List<TodayMatch>,
    ) {
        cachedParseMutex.withLock {
            cachedRawJson = rawJson
            cachedFavoriteTeamNames = favoriteTeamNames
            cachedMatches = matches
        }
    }

    internal fun selectMatchesForHome(
        matches: List<TodayMatch>,
        favoriteTeamNames: Set<String> = emptySet(),
    ): List<TodayMatch> = FootballMatchParser.selectMatchesForHome(matches, favoriteTeamNames)

    companion object {
        val TURKEY_TIME_ZONE: ZoneId = ZoneId.of("Europe/Istanbul")
    }
}

private object FootballMatchParser {
    fun parse(
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
            throw FootballApiException(
                kind = FootballApiException.Kind.INVALID_RESPONSE,
                message = message,
            )
        }

        val response = root.optJSONArray("response")
            ?: throw FootballApiException(
                kind = FootballApiException.Kind.INVALID_RESPONSE,
                message = "API-Football yanıtında maç listesi bulunamadı.",
            )
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

        return selectMatchesForHome(matches, favoriteTeamNames)
    }

    fun selectMatchesForHome(
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
