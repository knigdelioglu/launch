package io.github.knigdelioglu.seyir.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets

class TeamSearchRepository {
    suspend fun searchTeams(
        apiKey: String,
        query: String,
    ): List<FavoriteTeam> = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "API-Football anahtarı ayarlanmamış." }

        val normalizedQuery = query.trim()
        require(normalizedQuery.length >= MIN_SEARCH_LENGTH) {
            "Takım aramak için en az $MIN_SEARCH_LENGTH karakter girin."
        }

        val searchValue = URLEncoder.encode(
            normalizedQuery,
            StandardCharsets.UTF_8.name(),
        )
        val connection = (URL("$BASE_URL/teams?search=$searchValue")
            .openConnection() as HttpURLConnection).apply {
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
                    "Takım araması başarısız (HTTP $responseCode).",
                )
            }

            parseResponse(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResponse(body: String): List<FavoriteTeam> {
        val root = JSONObject(body)
        val errors = root.opt("errors")
        if (errors is JSONObject && errors.length() > 0) {
            val message = errors.keys().asSequence()
                .mapNotNull { key -> errors.optString(key).takeIf(String::isNotBlank) }
                .joinToString(" • ")
                .ifBlank { "API-Football takım aramasını reddetti." }
            throw IllegalStateException(message)
        }

        val response = root.optJSONArray("response") ?: return emptyList()
        return buildList {
            for (index in 0 until response.length()) {
                val item = response.optJSONObject(index) ?: continue
                val team = item.optJSONObject("team") ?: continue
                val id = team.optInt("id", -1)
                val name = team.optString("name").trim()
                val country = team.optString("country").trim()
                if (id <= 0 || name.isBlank()) continue

                add(
                    FavoriteTeam(
                        id = id,
                        name = name,
                        country = country,
                    ),
                )
            }
        }
            .distinctBy { it.id }
            .take(MAX_RESULTS)
    }

    private companion object {
        const val BASE_URL = "https://v3.football.api-sports.io"
        const val CONNECT_TIMEOUT_MS = 8_000
        const val READ_TIMEOUT_MS = 10_000
        const val MIN_SEARCH_LENGTH = 3
        const val MAX_RESULTS = 16
    }
}
