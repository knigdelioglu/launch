package io.github.knigdelioglu.seyir.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class MotorsportKind {
    FORMULA_1,
    MOTOGP,
}

data class TodayMotorsportSession(
    val id: String,
    val sport: MotorsportKind,
    val eventName: String,
    val circuitName: String,
    val sessionName: String,
    val startEpochSeconds: Long,
)

data class MotorsportDayResult(
    val rawJson: String,
    val sessions: List<TodayMotorsportSession>,
)

class MotorsportRepository(
    private val httpGet: suspend (String) -> String = ::defaultHttpGet,
) : MotorsportSource {
    override suspend fun loadTodaySessions(
        zoneId: ZoneId,
        date: LocalDate,
    ): MotorsportDayResult {
        val sessions = buildList {
            val f1 = runCatching {
                loadFormula1Sessions(zoneId, date)
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                emptyList()
            }
            addAll(f1)

            val motogp = runCatching {
                loadMotoGpSessions(zoneId, date)
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                emptyList()
            }
            addAll(motogp)
        }.sortedWith(
            compareBy<TodayMotorsportSession> { it.startEpochSeconds }
                .thenBy { it.sport.name }
                .thenBy { it.id },
        )

        return MotorsportDayResult(
            rawJson = encodeSessions(sessions),
            sessions = sessions,
        )
    }

    override suspend fun parseCachedSessions(rawJson: String): List<TodayMotorsportSession> =
        withContext(Dispatchers.Default) {
            decodeSessions(rawJson)
        }

    private suspend fun loadFormula1Sessions(
        zoneId: ZoneId,
        date: LocalDate,
    ): List<TodayMotorsportSession> {
        val raw = httpGet(
            "$MOTO_DB_BASE/data/f1/${date.year}/schedule.json",
        )
        val races = JSONObject(raw).optJSONArray("races") ?: return emptyList()
        val result = mutableListOf<TodayMotorsportSession>()

        for (index in 0 until races.length()) {
            val race = races.optJSONObject(index) ?: continue
            val eventName = race.optString("raceName").trim()
            val circuitName = race
                .optJSONObject("Circuit")
                ?.optString("circuitName")
                .orEmpty()
                .trim()
            val round = race.optString("round").ifBlank { index.toString() }

            fun addFlatSession(field: String, displayName: String) {
                val session = race.optJSONObject(field) ?: return
                addFormula1Session(
                    target = result,
                    zoneId = zoneId,
                    targetDate = date,
                    dateValue = session.optString("date"),
                    timeValue = session.optString("time"),
                    eventName = eventName,
                    circuitName = circuitName,
                    sessionName = displayName,
                    id = "f1-$round-$field",
                )
            }

            addFlatSession("FirstPractice", "Antrenman 1")
            addFlatSession("SecondPractice", "Antrenman 2")
            addFlatSession("ThirdPractice", "Antrenman 3")
            addFlatSession("SprintQualifying", "Sprint sıralama")
            addFlatSession("Sprint", "Sprint")
            addFlatSession("Qualifying", "Sıralama")

            addFormula1Session(
                target = result,
                zoneId = zoneId,
                targetDate = date,
                dateValue = race.optString("date"),
                timeValue = race.optString("time"),
                eventName = eventName,
                circuitName = circuitName,
                sessionName = "Yarış",
                id = "f1-$round-race",
            )
        }

        return result
    }

    private fun addFormula1Session(
        target: MutableList<TodayMotorsportSession>,
        zoneId: ZoneId,
        targetDate: LocalDate,
        dateValue: String,
        timeValue: String,
        eventName: String,
        circuitName: String,
        sessionName: String,
        id: String,
    ) {
        val epochSeconds = parseUtcDateTime(dateValue, timeValue) ?: return
        val localDate = Instant.ofEpochSecond(epochSeconds).atZone(zoneId).toLocalDate()
        if (localDate != targetDate) return

        target += TodayMotorsportSession(
            id = id,
            sport = MotorsportKind.FORMULA_1,
            eventName = eventName,
            circuitName = circuitName,
            sessionName = sessionName,
            startEpochSeconds = epochSeconds,
        )
    }

    private suspend fun loadMotoGpSessions(
        zoneId: ZoneId,
        date: LocalDate,
    ): List<TodayMotorsportSession> {
        val raw = httpGet(
            "$MOTO_DB_BASE/data/motogp/${date.year}/schedule.json",
        )
        val events = JSONObject(raw).optJSONArray("events") ?: return emptyList()

        for (index in 0 until events.length()) {
            val event = events.optJSONObject(index) ?: continue
            if (event.optBoolean("test", false)) continue

            val start = event.optString("date_start").toLocalDateOrNull() ?: continue
            val end = event.optString("date_end").toLocalDateOrNull() ?: start
            if (date < start || date > end) continue

            val embedded = parseEmbeddedMotoGpSessions(
                event = event,
                zoneId = zoneId,
                date = date,
            )
            if (embedded.isNotEmpty()) return embedded

            return fetchMotoGpSessionsFromPulseLive(
                event = event,
                zoneId = zoneId,
                date = date,
            )
        }

        return emptyList()
    }

    private fun parseEmbeddedMotoGpSessions(
        event: JSONObject,
        zoneId: ZoneId,
        date: LocalDate,
    ): List<TodayMotorsportSession> {
        val sessions = event.optJSONArray("sessions") ?: return emptyList()
        val eventName = event.optString("name").trim()
        val circuitName = event.optJSONObject("circuit")?.optString("name").orEmpty().trim()
        val eventId = event.optString("id")
        val result = mutableListOf<TodayMotorsportSession>()

        for (index in 0 until sessions.length()) {
            val session = sessions.optJSONObject(index) ?: continue
            val category = session.optString("category")
                .replace("™", "")
                .trim()
                .lowercase()
            if (category != "motogp") continue

            val instant = session.optString("datetime").toInstantOrNull() ?: continue
            if (instant.atZone(zoneId).toLocalDate() != date) continue

            result += TodayMotorsportSession(
                id = "motogp-$eventId-${session.optString("type")}-$index",
                sport = MotorsportKind.MOTOGP,
                eventName = eventName,
                circuitName = circuitName,
                sessionName = motoGpSessionName(
                    type = session.optString("type"),
                    fallback = session.optString("name"),
                ),
                startEpochSeconds = instant.epochSecond,
            )
        }

        return result
    }

    private suspend fun fetchMotoGpSessionsFromPulseLive(
        event: JSONObject,
        zoneId: ZoneId,
        date: LocalDate,
    ): List<TodayMotorsportSession> {
        val eventId = event.optString("id").trim()
        if (eventId.isBlank()) return emptyList()

        val categoriesRaw = httpGet(
            "$MOTOGP_BASE/results/categories?eventUuid=${urlEncode(eventId)}",
        )
        val categories = JSONArray(categoriesRaw)
        var categoryId: String? = null
        for (index in 0 until categories.length()) {
            val category = categories.optJSONObject(index) ?: continue
            val name = category.optString("name")
                .replace("™", "")
                .trim()
            if (category.optInt("legacy_id", 0) == 3 || name.equals("MotoGP", ignoreCase = true)) {
                categoryId = category.optString("id").takeIf(String::isNotBlank)
                if (categoryId != null) break
            }
        }
        val resolvedCategoryId = categoryId ?: return emptyList()

        val sessionsRaw = httpGet(
            "$MOTOGP_BASE/results/sessions" +
                "?eventUuid=${urlEncode(eventId)}" +
                "&categoryUuid=${urlEncode(resolvedCategoryId)}",
        )
        val sessions = JSONArray(sessionsRaw)
        val eventName = event.optString("name").trim()
        val circuitName = event.optJSONObject("circuit")?.optString("name").orEmpty().trim()
        val result = mutableListOf<TodayMotorsportSession>()

        for (index in 0 until sessions.length()) {
            val session = sessions.optJSONObject(index) ?: continue
            val instant = session.optString("date").toInstantOrNull() ?: continue
            if (instant.atZone(zoneId).toLocalDate() != date) continue

            result += TodayMotorsportSession(
                id = "motogp-$eventId-${session.optString("id", index.toString())}",
                sport = MotorsportKind.MOTOGP,
                eventName = eventName,
                circuitName = circuitName,
                sessionName = motoGpSessionName(
                    type = session.optString("type"),
                    fallback = session.optString("type"),
                ),
                startEpochSeconds = instant.epochSecond,
            )
        }

        return result
    }

    companion object {
        internal fun encodeSessions(sessions: List<TodayMotorsportSession>): String {
            val array = JSONArray()
            sessions.forEach { session ->
                array.put(
                    JSONObject()
                        .put("id", session.id)
                        .put("sport", session.sport.name)
                        .put("eventName", session.eventName)
                        .put("circuitName", session.circuitName)
                        .put("sessionName", session.sessionName)
                        .put("startEpochSeconds", session.startEpochSeconds),
                )
            }
            return array.toString()
        }

        internal fun decodeSessions(rawJson: String): List<TodayMotorsportSession> {
            if (rawJson.isBlank()) return emptyList()
            return try {
                val array = JSONArray(rawJson)
                buildList {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        val sport = runCatching {
                            MotorsportKind.valueOf(item.optString("sport"))
                        }.getOrNull() ?: continue
                        val id = item.optString("id").trim()
                        val eventName = item.optString("eventName").trim()
                        val sessionName = item.optString("sessionName").trim()
                        val startEpochSeconds = item.optLong("startEpochSeconds", -1L)
                        if (
                            id.isBlank() ||
                            eventName.isBlank() ||
                            sessionName.isBlank() ||
                            startEpochSeconds <= 0L
                        ) {
                            continue
                        }

                        add(
                            TodayMotorsportSession(
                                id = id,
                                sport = sport,
                                eventName = eventName,
                                circuitName = item.optString("circuitName").trim(),
                                sessionName = sessionName,
                                startEpochSeconds = startEpochSeconds,
                            ),
                        )
                    }
                }
            } catch (_: JSONException) {
                emptyList()
            }
        }

        private const val MOTO_DB_BASE =
            "https://cdn.jsdelivr.net/gh/vishwapramuditha/moto-db@main"
        private const val MOTOGP_BASE =
            "https://api.motogp.pulselive.com/motogp/v1"

        private fun parseUtcDateTime(dateValue: String, timeValue: String): Long? {
            if (dateValue.isBlank() || timeValue.isBlank()) return null
            val normalizedTime = timeValue.removeSuffix("Z").takeIf(String::isNotBlank) ?: return null
            return runCatching {
                Instant.parse("${dateValue.trim()}T$normalizedTimeZ").epochSecond
            }.getOrNull()
        }

        private fun String.toInstantOrNull(): Instant? =
            runCatching { Instant.parse(trim()) }
                .recoverCatching { java.time.OffsetDateTime.parse(trim()).toInstant() }
                .getOrNull()

        private fun String.toLocalDateOrNull(): LocalDate? =
            runCatching { LocalDate.parse(trim()) }.getOrNull()

        private fun motoGpSessionName(type: String, fallback: String): String =
            when (type.trim().uppercase()) {
                "FP", "FP1" -> "Antrenman"
                "FP2" -> "Antrenman 2"
                "FP3" -> "Antrenman 3"
                "PR" -> "Antrenman"
                "Q1" -> "Sıralama 1"
                "Q2" -> "Sıralama 2"
                "SQ" -> "Sprint sıralama"
                "SPR" -> "Sprint"
                "WUP" -> "Isınma"
                "RAC", "RACE" -> "Yarış"
                else -> fallback
                    .replace("(MotoGP)", "", ignoreCase = true)
                    .trim()
                    .ifBlank { type.trim() }
            }

        private fun urlEncode(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name())

        private suspend fun defaultHttpGet(url: String): String =
            withContext(Dispatchers.IO) {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "Seyir/1.0 AndroidTV launcher")
                }
                try {
                    val responseCode = connection.responseCode
                    val stream = if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                    val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (responseCode !in 200..299 || body.isBlank()) {
                        throw IOException("Motorsport feed HTTP $responseCode")
                    }
                    body
                } finally {
                    connection.disconnect()
                }
            }
    }
}
