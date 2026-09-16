package io.github.knigdelioglu.seyir.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime
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
        zoneId: ZoneId = ZoneId.systemDefault(),
        date: LocalDate = LocalDate.now(zoneId),
        favoriteTeamNames: Set<String> = emptySet(),
    ): DailyMatchResult = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Gemini API anahtarı ayarlanmamış." }

        val connection = (URL(INTERACTIONS_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("x-goog-api-key", apiKey.trim())
        }

        try {
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(buildRequest(date, zoneId, favoriteTeamNames).toString())
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299) {
                val message = runCatching {
                    JSONObject(body)
                        .optJSONObject("error")
                        ?.optString("message")
                        ?.takeIf(String::isNotBlank)
                }.getOrNull()
                throw IllegalStateException(
                    message ?: "Gemini maç verisi alınamadı (HTTP $responseCode).",
                )
            }

            val outputJson = extractOutputText(body)
                ?: throw IllegalStateException("Gemini yapılandırılmış maç verisi döndürmedi.")
            val matches = parseCachedMatches(
                rawJson = outputJson,
                date = date,
                zoneId = zoneId,
                favoriteTeamNames = favoriteTeamNames,
            )
            DailyMatchResult(rawJson = outputJson, matches = matches)
        } finally {
            connection.disconnect()
        }
    }

    fun parseCachedMatches(
        rawJson: String,
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
        favoriteTeamNames: Set<String> = emptySet(),
    ): List<TodayMatch> {
        if (rawJson.isBlank()) return emptyList()

        val root = JSONObject(rawJson)
        val response = root.optJSONArray("matches") ?: return emptyList()
        val matches = buildList {
            for (index in 0 until response.length()) {
                val item = response.optJSONObject(index) ?: continue
                val league = item.optString("league").trim()
                val country = item.optString("country").trim()
                val home = item.optString("home_team").trim()
                val away = item.optString("away_team").trim()
                val kickoffLocal = item.optString("kickoff_local").trim()
                if (home.isBlank() || away.isBlank() || kickoffLocal.isBlank()) continue

                val localTime = runCatching { LocalTime.parse(kickoffLocal) }.getOrNull() ?: continue
                val kickoffEpochSeconds = date
                    .atTime(localTime)
                    .atZone(zoneId)
                    .toEpochSecond()
                val status = item.optString("status", "scheduled").lowercase(Locale.ROOT)
                val statusShort = when (status) {
                    "live" -> "LIVE"
                    "finished" -> "FT"
                    "postponed" -> "PST"
                    "cancelled" -> "CANC"
                    else -> "NS"
                }

                add(
                    TodayMatch(
                        fixtureId = stableFixtureId(date, league, home, away, kickoffLocal),
                        leagueName = league.ifBlank { "Futbol" },
                        countryName = country,
                        homeTeam = home,
                        awayTeam = away,
                        kickoffEpochSeconds = kickoffEpochSeconds,
                        statusShort = statusShort,
                        statusLong = status,
                        elapsedMinute = item.nullableInt("elapsed_minute"),
                        homeGoals = item.nullableInt("home_goals"),
                        awayGoals = item.nullableInt("away_goals"),
                    ),
                )
            }
        }

        val normalizedFavorites = favoriteTeamNames
            .asSequence()
            .map(::normalizeName)
            .filter(String::isNotBlank)
            .toSet()

        return matches
            .distinctBy { it.fixtureId }
            .sortedWith(
                compareBy<TodayMatch> { if (it.involvesFavorite(normalizedFavorites)) 0 else 1 }
                    .thenBy(::competitionPriority)
                    .thenBy { it.kickoffEpochSeconds },
            )
            .take(MAX_HOME_MATCHES)
    }

    private fun buildRequest(
        date: LocalDate,
        zoneId: ZoneId,
        favoriteTeamNames: Set<String>,
    ): JSONObject {
        val favorites = favoriteTeamNames
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(", ")
            .ifBlank { "yok" }

        val prompt = """
            Bugün $date. Kullanıcının saat dilimi ${zoneId.id}.
            Google Search kullanarak yalnızca bu tarihte oynanan veya oynanacak futbol maçlarını bul.
            Türkiye ligleri ve kupaları, UEFA kulüp turnuvaları ve büyük Avrupa liglerindeki öne çıkan maçlara öncelik ver.
            Kullanıcının Takımlarım listesi: $favorites.
            Takımlarım listesindeki bir takım bugün oynuyorsa o maçı mutlaka dahil et.
            En fazla $MAX_HOME_MATCHES maç döndür. Emin olmadığın karşılaşmayı uydurma.
            kickoff_local alanını kullanıcının saat diliminde kesin HH:mm biçiminde ver.
            status yalnız scheduled, live, finished, postponed veya cancelled değerlerinden biri olsun.
            Skor bilinmiyorsa gol alanlarını null bırak.
        """.trimIndent()

        return JSONObject()
            .put("model", MODEL)
            .put("input", prompt)
            .put("store", false)
            .put(
                "tools",
                JSONArray().put(JSONObject().put("type", "google_search")),
            )
            .put(
                "response_format",
                JSONObject()
                    .put("type", "text")
                    .put("mime_type", "application/json")
                    .put("schema", matchSchema()),
            )
    }

    private fun matchSchema(): JSONObject {
        fun nullableInteger(): JSONObject = JSONObject().put(
            "type",
            JSONArray().put("integer").put("null"),
        )

        val itemProperties = JSONObject()
            .put("league", JSONObject().put("type", "string"))
            .put("country", JSONObject().put("type", "string"))
            .put("home_team", JSONObject().put("type", "string"))
            .put("away_team", JSONObject().put("type", "string"))
            .put("kickoff_local", JSONObject().put("type", "string"))
            .put(
                "status",
                JSONObject()
                    .put("type", "string")
                    .put(
                        "enum",
                        JSONArray()
                            .put("scheduled")
                            .put("live")
                            .put("finished")
                            .put("postponed")
                            .put("cancelled"),
                    ),
            )
            .put("elapsed_minute", nullableInteger())
            .put("home_goals", nullableInteger())
            .put("away_goals", nullableInteger())

        val matchItem = JSONObject()
            .put("type", "object")
            .put("properties", itemProperties)
            .put(
                "required",
                JSONArray()
                    .put("league")
                    .put("country")
                    .put("home_team")
                    .put("away_team")
                    .put("kickoff_local")
                    .put("status")
                    .put("elapsed_minute")
                    .put("home_goals")
                    .put("away_goals"),
            )

        return JSONObject()
            .put("type", "object")
            .put(
                "properties",
                JSONObject().put(
                    "matches",
                    JSONObject()
                        .put("type", "array")
                        .put("items", matchItem),
                ),
            )
            .put("required", JSONArray().put("matches"))
    }

    private fun extractOutputText(body: String): String? {
        val root = JSONObject(body)
        root.optString("output_text")
            .takeIf(String::isNotBlank)
            ?.let { return it }

        val steps = root.optJSONArray("steps") ?: return null
        for (stepIndex in steps.length() - 1 downTo 0) {
            val step = steps.optJSONObject(stepIndex) ?: continue
            if (step.optString("type") != "model_output") continue
            val content = step.optJSONArray("content") ?: continue
            for (contentIndex in content.length() - 1 downTo 0) {
                val text = content.optJSONObject(contentIndex)
                    ?.optString("text")
                    ?.takeIf(String::isNotBlank)
                if (text != null) return text
            }
        }
        return null
    }

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

    private fun competitionPriority(match: TodayMatch): Int {
        val league = normalizeName(match.leagueName)
        val country = normalizeName(match.countryName)

        return when {
            "super lig" in league ||
                "turkiye kupasi" in league ||
                "champions league" in league ||
                "europa league" in league ||
                "conference league" in league -> 0

            country == "turkey" || country == "turkiye" -> 1
            league in TOP_EUROPEAN_LEAGUES -> 2
            else -> 3
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

    private fun stableFixtureId(
        date: LocalDate,
        league: String,
        home: String,
        away: String,
        kickoff: String,
    ): Long = "$date|$league|$home|$away|$kickoff".hashCode().toLong() and 0xffffffffL

    private fun JSONObject.nullableInt(key: String): Int? =
        if (!has(key) || isNull(key)) null else optInt(key)

    private companion object {
        const val INTERACTIONS_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"
        const val MODEL = "gemini-3.6-flash"
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 30_000
        const val MAX_HOME_MATCHES = 12

        val TOP_EUROPEAN_LEAGUES = setOf(
            "premier league",
            "la liga",
            "serie a",
            "bundesliga",
            "ligue 1",
        )
    }
}
