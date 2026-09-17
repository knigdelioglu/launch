package io.github.knigdelioglu.seyir.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.knigdelioglu.seyir.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.launcherPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "launcher_preferences",
)

enum class ThemeMode {
    DARK,
    BLACK,
}

enum class AccentMode {
    NEUTRAL,
    BLUE,
    EMERALD,
}

data class FavoriteTeam(
    val id: Int,
    val name: String,
    val country: String,
)

data class LauncherPreferences(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val favoritesInitialized: Boolean = false,
    val favoritePackages: List<String> = emptyList(),
    val hiddenPackages: Set<String> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentMode: AccentMode = AccentMode.NEUTRAL,
    val reducedMotion: Boolean = false,
    val geminiApiKey: String = BuildConfig.FOOTBALL_API_KEY,
    val favoriteTeams: List<FavoriteTeam> = emptyList(),
    val dailyMatchAttemptDate: String = "",
    val dailyMatchCacheDate: String = "",
    val dailyMatchCacheJson: String = "",
    val dailyMatchCacheFetchedAtMillis: Long = 0L,
)

class LauncherPreferencesRepository(
    context: Context,
) {
    private val dataStore = context.launcherPreferencesDataStore

    val preferences: Flow<LauncherPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map(::decode)

    suspend fun initializeFavoritesIfNeeded(packageNames: List<String>) {
        dataStore.edit { preferences ->
            if (preferences[FAVORITES_INITIALIZED] == true) return@edit

            preferences[SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            preferences[FAVORITE_PACKAGES] = encodeOrderedPackages(
                packageNames.take(DEFAULT_FAVORITE_COUNT),
            )
            preferences[FAVORITES_INITIALIZED] = true
        }
    }

    suspend fun setFavorites(packageNames: List<String>) {
        dataStore.edit { preferences ->
            preferences[SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            preferences[FAVORITE_PACKAGES] = encodeOrderedPackages(packageNames)
            preferences[FAVORITES_INITIALIZED] = true
        }
    }

    suspend fun toggleFavorite(packageName: String): Boolean {
        var isFavoriteAfterUpdate = false

        dataStore.edit { preferences ->
            val favorites = decodeOrderedPackages(preferences[FAVORITE_PACKAGES]).toMutableList()

            isFavoriteAfterUpdate = if (packageName in favorites) {
                favorites.remove(packageName)
                false
            } else {
                favorites.add(packageName)
                true
            }

            preferences[SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            preferences[FAVORITE_PACKAGES] = encodeOrderedPackages(favorites)
            preferences[FAVORITES_INITIALIZED] = true
        }

        return isFavoriteAfterUpdate
    }

    suspend fun setHidden(packageName: String, hidden: Boolean) {
        dataStore.edit { preferences ->
            val hiddenPackages = preferences[HIDDEN_PACKAGES].orEmpty().toMutableSet()
            if (hidden) {
                hiddenPackages.add(packageName)
            } else {
                hiddenPackages.remove(packageName)
            }

            preferences[SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            preferences[HIDDEN_PACKAGES] = hiddenPackages
        }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            preferences[THEME_MODE] = themeMode.name
        }
    }

    suspend fun setAccentMode(accentMode: AccentMode) {
        dataStore.edit { preferences ->
            preferences[SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            preferences[ACCENT_MODE] = accentMode.name
        }
    }

    suspend fun setReducedMotion(reducedMotion: Boolean) {
        dataStore.edit { preferences ->
            preferences[SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            preferences[REDUCED_MOTION] = reducedMotion
        }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            preferences[GEMINI_API_KEY] = apiKey.trim()
            preferences[API_FOOTBALL_KEY] = apiKey.trim()
        }
    }

    suspend fun markDailyMatchAttempt(date: String) {
        dataStore.edit { preferences ->
            preferences[SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            preferences[DAILY_MATCH_ATTEMPT_DATE] = date
        }
    }

    suspend fun saveDailyMatchCache(
        date: String,
        json: String,
        fetchedAtMillis: Long,
    ) {
        dataStore.edit { preferences ->
            preferences[SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            preferences[DAILY_MATCH_CACHE_DATE] = date
            preferences[DAILY_MATCH_CACHE_JSON] = json
            preferences[DAILY_MATCH_CACHE_FETCHED_AT] = fetchedAtMillis
        }
    }

    suspend fun toggleFavoriteTeam(team: FavoriteTeam): Boolean {
        var selectedAfterUpdate = false

        dataStore.edit { preferences ->
            val teams = decodeFavoriteTeams(preferences[FAVORITE_TEAMS]).toMutableList()
            val existingIndex = teams.indexOfFirst { it.id == team.id }

            selectedAfterUpdate = if (existingIndex >= 0) {
                teams.removeAt(existingIndex)
                false
            } else {
                teams.add(team.copy(name = team.name.trim(), country = team.country.trim()))
                true
            }

            preferences[SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            preferences[FAVORITE_TEAMS] = encodeFavoriteTeams(teams)
        }

        return selectedAfterUpdate
    }

    suspend fun cleanupUnavailablePackages(availablePackages: Set<String>) {
        dataStore.edit { preferences ->
            val favorites = decodeOrderedPackages(preferences[FAVORITE_PACKAGES])
                .filter { it in availablePackages }
            val hidden = preferences[HIDDEN_PACKAGES].orEmpty()
                .filterTo(linkedSetOf()) { it in availablePackages }

            preferences[SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            preferences[FAVORITE_PACKAGES] = encodeOrderedPackages(favorites)
            preferences[HIDDEN_PACKAGES] = hidden
        }
    }

    private fun decode(preferences: Preferences): LauncherPreferences = LauncherPreferences(
        schemaVersion = preferences[SCHEMA_VERSION] ?: CURRENT_SCHEMA_VERSION,
        favoritesInitialized = preferences[FAVORITES_INITIALIZED] ?: false,
        favoritePackages = decodeOrderedPackages(preferences[FAVORITE_PACKAGES]),
        hiddenPackages = preferences[HIDDEN_PACKAGES].orEmpty(),
        themeMode = preferences[THEME_MODE].toEnumOrDefault(ThemeMode.DARK),
        accentMode = preferences[ACCENT_MODE].toEnumOrDefault(AccentMode.NEUTRAL),
        reducedMotion = preferences[REDUCED_MOTION] ?: false,
        geminiApiKey = preferences[API_FOOTBALL_KEY]?.takeIf { it.isNotBlank() }
            ?: preferences[GEMINI_API_KEY]?.takeIf { it.isNotBlank() }
            ?: BuildConfig.FOOTBALL_API_KEY,
        favoriteTeams = decodeFavoriteTeams(preferences[FAVORITE_TEAMS]),
        dailyMatchAttemptDate = preferences[DAILY_MATCH_ATTEMPT_DATE].orEmpty(),
        dailyMatchCacheDate = preferences[DAILY_MATCH_CACHE_DATE].orEmpty(),
        dailyMatchCacheJson = preferences[DAILY_MATCH_CACHE_JSON].orEmpty(),
        dailyMatchCacheFetchedAtMillis = preferences[DAILY_MATCH_CACHE_FETCHED_AT] ?: 0L,
    )

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
        this?.let { encoded ->
            enumValues<T>().firstOrNull { it.name == encoded }
        } ?: default

    private fun encodeOrderedPackages(packageNames: List<String>): String = packageNames
        .asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .joinToString(PACKAGE_SEPARATOR)

    private fun decodeOrderedPackages(encoded: String?): List<String> = encoded
        .orEmpty()
        .split(PACKAGE_SEPARATOR)
        .asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .toList()

    private fun encodeFavoriteTeams(teams: List<FavoriteTeam>): String = teams
        .asSequence()
        .distinctBy { it.id }
        .joinToString(PACKAGE_SEPARATOR) { team ->
            listOf(
                team.id.toString(),
                sanitizeTeamField(team.name),
                sanitizeTeamField(team.country),
            ).joinToString(TEAM_FIELD_SEPARATOR)
        }

    private fun decodeFavoriteTeams(encoded: String?): List<FavoriteTeam> = encoded
        .orEmpty()
        .split(PACKAGE_SEPARATOR)
        .mapNotNull { record ->
            val fields = record.split(TEAM_FIELD_SEPARATOR)
            val id = fields.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val name = fields.getOrNull(1).orEmpty().trim()
            val country = fields.getOrNull(2).orEmpty().trim()
            if (name.isBlank()) return@mapNotNull null
            FavoriteTeam(id = id, name = name, country = country)
        }
        .distinctBy { it.id }

    private fun sanitizeTeamField(value: String): String = value
        .replace(PACKAGE_SEPARATOR, " ")
        .replace(TEAM_FIELD_SEPARATOR, " ")
        .trim()

    private companion object {
        const val DEFAULT_FAVORITE_COUNT = 7
        const val PACKAGE_SEPARATOR = "\n"
        const val TEAM_FIELD_SEPARATOR = "\u001F"

        val SCHEMA_VERSION = intPreferencesKey("schema_version")
        val FAVORITES_INITIALIZED = booleanPreferencesKey("favorites_initialized")
        val FAVORITE_PACKAGES = stringPreferencesKey("favorite_packages_v1")
        val HIDDEN_PACKAGES = stringSetPreferencesKey("hidden_packages_v1")
        val THEME_MODE = stringPreferencesKey("theme_mode_v1")
        val ACCENT_MODE = stringPreferencesKey("accent_mode_v1")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion_v1")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key_v1")
        val API_FOOTBALL_KEY = stringPreferencesKey("api_football_key_v1")
        val FAVORITE_TEAMS = stringPreferencesKey("favorite_teams_v1")
        val DAILY_MATCH_ATTEMPT_DATE = stringPreferencesKey("daily_match_attempt_date_v1")
        val DAILY_MATCH_CACHE_DATE = stringPreferencesKey("daily_match_cache_date_v1")
        val DAILY_MATCH_CACHE_JSON = stringPreferencesKey("daily_match_cache_json_v1")
        val DAILY_MATCH_CACHE_FETCHED_AT = longPreferencesKey("daily_match_cache_fetched_at_v1")
    }
}

private const val CURRENT_SCHEMA_VERSION = 5
