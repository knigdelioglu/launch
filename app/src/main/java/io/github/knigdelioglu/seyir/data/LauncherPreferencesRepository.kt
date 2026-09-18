package io.github.knigdelioglu.seyir.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.io.IOException

private val Context.launcherPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "launcher_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
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

internal const val DEFAULT_DARK_MODE_START_MINUTES = 20 * 60
internal const val DEFAULT_DARK_MODE_END_MINUTES = 7 * 60

data class FavoriteTeam(
    val id: Int,
    val name: String,
    val country: String,
)

data class LauncherPreferences(
    val schemaVersion: Int = CURRENT_LAUNCHER_SCHEMA_VERSION,
    val favoritesInitialized: Boolean = false,
    val favoritePackages: List<String> = emptyList(),
    val hiddenPackages: Set<String> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    val darkModeScheduleEnabled: Boolean = false,
    val darkModeStartMinutes: Int = DEFAULT_DARK_MODE_START_MINUTES,
    val darkModeEndMinutes: Int = DEFAULT_DARK_MODE_END_MINUTES,
    val accentMode: AccentMode = AccentMode.NEUTRAL,
    val reducedMotion: Boolean = false,
    val footballApiKey: String = BuildConfig.FOOTBALL_API_KEY,
    val favoriteTeams: List<FavoriteTeam> = emptyList(),
    val dailyMatchAttemptDate: String = "",
    val dailyMatchCacheDate: String = "",
    val dailyMatchCacheJson: String = "",
    val dailyMatchCacheFetchedAtMillis: Long = 0L,
)

class LauncherPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
    private val defaultApiKey: String = BuildConfig.FOOTBALL_API_KEY,
) : LauncherPreferencesSource {
    constructor(context: Context) : this(
        dataStore = context.launcherPreferencesDataStore,
        defaultApiKey = BuildConfig.FOOTBALL_API_KEY,
    )

    override val preferences: Flow<LauncherPreferences> = dataStore.data
        .onStart { migrate() }
        .catch { error ->
            when (error) {
                is CancellationException -> throw error
                is IOException -> emit(emptyPreferences())
                else -> throw error
            }
        }
        .map { preferences ->
            LauncherPreferencesCodec.decode(
                preferences = preferences,
                defaultApiKey = defaultApiKey,
            )
        }

    /**
     * DataStore has no schema callback. Migration runs before the first read
     * and before every write, keeping old v1-v4 records readable without
     * changing the serialized keys in place.
     */
    internal suspend fun migrate() {
        dataStore.edit { preferences ->
            LauncherPreferencesCodec.migrate(preferences)
        }
    }

    override suspend fun initializeFavoritesIfNeeded(packageNames: List<String>) {
        updatePreferences { preferences ->
            if (preferences[FAVORITES_INITIALIZED] == true) return@updatePreferences

            preferences[FAVORITE_PACKAGES] = LauncherPreferencesCodec.encodeOrderedPackages(
                packageNames.take(DEFAULT_FAVORITE_COUNT),
            )
            preferences[FAVORITES_INITIALIZED] = true
        }
    }

    override suspend fun setFavorites(packageNames: List<String>) {
        updatePreferences { preferences ->
            preferences[FAVORITE_PACKAGES] = LauncherPreferencesCodec.encodeOrderedPackages(packageNames)
            preferences[FAVORITES_INITIALIZED] = true
        }
    }

    override suspend fun toggleFavorite(packageName: String): Boolean {
        val normalizedPackageName = packageName.trim()
        if (normalizedPackageName.isBlank()) return false

        var isFavoriteAfterUpdate = false
        updatePreferences { preferences ->
            val favorites = LauncherPreferencesCodec
                .decodeOrderedPackages(preferences[FAVORITE_PACKAGES])
                .toMutableList()

            isFavoriteAfterUpdate = if (normalizedPackageName in favorites) {
                favorites.remove(normalizedPackageName)
                false
            } else {
                favorites.add(normalizedPackageName)
                true
            }

            preferences[FAVORITE_PACKAGES] = LauncherPreferencesCodec.encodeOrderedPackages(favorites)
            preferences[FAVORITES_INITIALIZED] = true
        }

        return isFavoriteAfterUpdate
    }

    override suspend fun setHidden(packageName: String, hidden: Boolean) {
        val normalizedPackageName = packageName.trim()
        if (normalizedPackageName.isBlank()) return

        updatePreferences { preferences ->
            val hiddenPackages = preferences[HIDDEN_PACKAGES].orEmpty().toMutableSet()
            if (hidden) {
                hiddenPackages.add(normalizedPackageName)
            } else {
                hiddenPackages.remove(normalizedPackageName)
            }
            preferences[HIDDEN_PACKAGES] = hiddenPackages
        }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        updatePreferences { preferences -> preferences[THEME_MODE] = themeMode.name }
    }

    override suspend fun setDarkModeSchedule(
        enabled: Boolean,
        startMinutes: Int,
        endMinutes: Int,
    ) {
        updatePreferences { preferences ->
            preferences[DARK_MODE_SCHEDULE_ENABLED] = enabled
            preferences[DARK_MODE_START_MINUTES] = startMinutes.coerceIn(0, MINUTES_PER_DAY - 1)
            preferences[DARK_MODE_END_MINUTES] = endMinutes.coerceIn(0, MINUTES_PER_DAY - 1)
        }
    }

    override suspend fun setAccentMode(accentMode: AccentMode) {
        updatePreferences { preferences -> preferences[ACCENT_MODE] = accentMode.name }
    }

    override suspend fun setReducedMotion(reducedMotion: Boolean) {
        updatePreferences { preferences -> preferences[REDUCED_MOTION] = reducedMotion }
    }

    override suspend fun setFootballApiKey(apiKey: String) {
        updatePreferences { preferences -> preferences[API_FOOTBALL_KEY] = apiKey.trim() }
    }

    override suspend fun markDailyMatchAttempt(date: String) {
        updatePreferences { preferences -> preferences[DAILY_MATCH_ATTEMPT_DATE] = date.trim() }
    }

    override suspend fun saveDailyMatchCache(
        date: String,
        json: String,
        fetchedAtMillis: Long,
    ) {
        updatePreferences { preferences ->
            preferences[DAILY_MATCH_CACHE_DATE] = date.trim()
            preferences[DAILY_MATCH_CACHE_JSON] = json
            preferences[DAILY_MATCH_CACHE_FETCHED_AT] = fetchedAtMillis
        }
    }

    override suspend fun toggleFavoriteTeam(team: FavoriteTeam): Boolean {
        if (team.id <= 0 || team.name.isBlank()) return false

        var selectedAfterUpdate = false
        updatePreferences { preferences ->
            val teams = LauncherPreferencesCodec
                .decodeFavoriteTeams(preferences[FAVORITE_TEAMS])
                .toMutableList()
            val existingIndex = teams.indexOfFirst { it.id == team.id }

            selectedAfterUpdate = if (existingIndex >= 0) {
                teams.removeAt(existingIndex)
                false
            } else {
                teams.add(team.copy(name = team.name.trim(), country = team.country.trim()))
                true
            }

            preferences[FAVORITE_TEAMS] = LauncherPreferencesCodec.encodeFavoriteTeams(teams)
        }

        return selectedAfterUpdate
    }

    override suspend fun cleanupUnavailablePackages(availablePackages: Set<String>) {
        val normalizedAvailable = availablePackages.mapTo(hashSetOf(), String::trim)
        updatePreferences { preferences ->
            val favorites = LauncherPreferencesCodec
                .decodeOrderedPackages(preferences[FAVORITE_PACKAGES])
                .filter { it in normalizedAvailable }
            val hidden = preferences[HIDDEN_PACKAGES].orEmpty()
                .filterTo(linkedSetOf()) { it in normalizedAvailable }

            preferences[FAVORITE_PACKAGES] = LauncherPreferencesCodec.encodeOrderedPackages(favorites)
            preferences[HIDDEN_PACKAGES] = hidden
        }
    }

    private suspend fun updatePreferences(block: (MutablePreferences) -> Unit) {
        dataStore.edit { preferences ->
            LauncherPreferencesCodec.migrate(preferences)
            block(preferences)
        }
    }

    private companion object {
        const val DEFAULT_FAVORITE_COUNT = 7

        val FAVORITES_INITIALIZED = booleanPreferencesKey("favorites_initialized")
        val FAVORITE_PACKAGES = stringPreferencesKey("favorite_packages_v1")
        val HIDDEN_PACKAGES = stringSetPreferencesKey("hidden_packages_v1")
        val THEME_MODE = stringPreferencesKey("theme_mode_v1")
        val DARK_MODE_SCHEDULE_ENABLED = booleanPreferencesKey("dark_mode_schedule_enabled_v1")
        val DARK_MODE_START_MINUTES = intPreferencesKey("dark_mode_start_minutes_v1")
        val DARK_MODE_END_MINUTES = intPreferencesKey("dark_mode_end_minutes_v1")
        val ACCENT_MODE = stringPreferencesKey("accent_mode_v1")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion_v1")
        val API_FOOTBALL_KEY = stringPreferencesKey("api_football_key_v1")
        val FAVORITE_TEAMS = stringPreferencesKey("favorite_teams_v1")
        val DAILY_MATCH_ATTEMPT_DATE = stringPreferencesKey("daily_match_attempt_date_v1")
        val DAILY_MATCH_CACHE_DATE = stringPreferencesKey("daily_match_cache_date_v1")
        val DAILY_MATCH_CACHE_JSON = stringPreferencesKey("daily_match_cache_json_v1")
        val DAILY_MATCH_CACHE_FETCHED_AT = longPreferencesKey("daily_match_cache_fetched_at_v1")
        const val MINUTES_PER_DAY = 24 * 60
    }
}

internal const val CURRENT_LAUNCHER_SCHEMA_VERSION = 6

internal object LauncherPreferencesCodec {
    private const val PACKAGE_SEPARATOR = "\n"
    private const val TEAM_FIELD_SEPARATOR = "\u001F"

    private val SCHEMA_VERSION = intPreferencesKey("schema_version")
    private val FAVORITES_INITIALIZED = booleanPreferencesKey("favorites_initialized")
    private val FAVORITE_PACKAGES = stringPreferencesKey("favorite_packages_v1")
    private val HIDDEN_PACKAGES = stringSetPreferencesKey("hidden_packages_v1")
    private val THEME_MODE = stringPreferencesKey("theme_mode_v1")
    private val DARK_MODE_SCHEDULE_ENABLED = booleanPreferencesKey("dark_mode_schedule_enabled_v1")
    private val DARK_MODE_START_MINUTES = intPreferencesKey("dark_mode_start_minutes_v1")
    private val DARK_MODE_END_MINUTES = intPreferencesKey("dark_mode_end_minutes_v1")
    private val ACCENT_MODE = stringPreferencesKey("accent_mode_v1")
    private val REDUCED_MOTION = booleanPreferencesKey("reduced_motion_v1")
    private val API_FOOTBALL_KEY = stringPreferencesKey("api_football_key_v1")
    private val FAVORITE_TEAMS = stringPreferencesKey("favorite_teams_v1")
    private val DAILY_MATCH_ATTEMPT_DATE = stringPreferencesKey("daily_match_attempt_date_v1")
    private val DAILY_MATCH_CACHE_DATE = stringPreferencesKey("daily_match_cache_date_v1")
    private val DAILY_MATCH_CACHE_JSON = stringPreferencesKey("daily_match_cache_json_v1")
    private val DAILY_MATCH_CACHE_FETCHED_AT = longPreferencesKey("daily_match_cache_fetched_at_v1")

    internal fun migrate(preferences: MutablePreferences) {
        val storedVersion = preferences[SCHEMA_VERSION]
        if (storedVersion == null || storedVersion < CURRENT_LAUNCHER_SCHEMA_VERSION) {
            // v1-v4 used the same keys and separators. The explicit migration
            // records that those values have been normalized to the current schema.
            preferences[SCHEMA_VERSION] = CURRENT_LAUNCHER_SCHEMA_VERSION
        }
    }

    internal fun decode(
        preferences: Preferences,
        defaultApiKey: String,
    ): LauncherPreferences = LauncherPreferences(
        schemaVersion = preferences[SCHEMA_VERSION] ?: CURRENT_LAUNCHER_SCHEMA_VERSION,
        favoritesInitialized = preferences[FAVORITES_INITIALIZED] ?: false,
        favoritePackages = decodeOrderedPackages(preferences[FAVORITE_PACKAGES]),
        hiddenPackages = preferences[HIDDEN_PACKAGES].orEmpty(),
        themeMode = preferences[THEME_MODE].toEnumOrDefault(ThemeMode.DARK),
        darkModeScheduleEnabled = preferences[DARK_MODE_SCHEDULE_ENABLED] ?: false,
        darkModeStartMinutes = preferences[DARK_MODE_START_MINUTES]
            ?.coerceIn(0, MINUTES_PER_DAY - 1)
            ?: DEFAULT_DARK_MODE_START_MINUTES,
        darkModeEndMinutes = preferences[DARK_MODE_END_MINUTES]
            ?.coerceIn(0, MINUTES_PER_DAY - 1)
            ?: DEFAULT_DARK_MODE_END_MINUTES,
        accentMode = preferences[ACCENT_MODE].toEnumOrDefault(AccentMode.NEUTRAL),
        reducedMotion = preferences[REDUCED_MOTION] ?: false,
        // A stored blank value intentionally disables the feature. The build-time
        // key is only the fallback for installations with no stored value yet.
        footballApiKey = preferences[API_FOOTBALL_KEY]?.trim() ?: defaultApiKey.trim(),
        favoriteTeams = decodeFavoriteTeams(preferences[FAVORITE_TEAMS]),
        dailyMatchAttemptDate = preferences[DAILY_MATCH_ATTEMPT_DATE].orEmpty(),
        dailyMatchCacheDate = preferences[DAILY_MATCH_CACHE_DATE].orEmpty(),
        dailyMatchCacheJson = preferences[DAILY_MATCH_CACHE_JSON].orEmpty(),
        dailyMatchCacheFetchedAtMillis = preferences[DAILY_MATCH_CACHE_FETCHED_AT] ?: 0L,
    )

    internal fun encodeOrderedPackages(packageNames: List<String>): String = packageNames
        .asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .joinToString(PACKAGE_SEPARATOR)

    internal fun decodeOrderedPackages(encoded: String?): List<String> = encoded
        .orEmpty()
        .split(PACKAGE_SEPARATOR)
        .asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .toList()

    internal fun encodeFavoriteTeams(teams: List<FavoriteTeam>): String = teams
        .asSequence()
        .filter { it.id > 0 && it.name.isNotBlank() }
        .distinctBy { it.id }
        .joinToString(PACKAGE_SEPARATOR) { team ->
            listOf(
                team.id.toString(),
                sanitizeTeamField(team.name),
                sanitizeTeamField(team.country),
            ).joinToString(TEAM_FIELD_SEPARATOR)
        }

    internal fun decodeFavoriteTeams(encoded: String?): List<FavoriteTeam> = encoded
        .orEmpty()
        .split(PACKAGE_SEPARATOR)
        .mapNotNull { record ->
            val fields = record.split(TEAM_FIELD_SEPARATOR, limit = 3)
            val id = fields.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val name = fields.getOrNull(1).orEmpty().trim()
            val country = fields.getOrNull(2).orEmpty().trim()
            if (id <= 0 || name.isBlank()) return@mapNotNull null
            FavoriteTeam(id = id, name = name, country = country)
        }
        .distinctBy { it.id }

    private fun sanitizeTeamField(value: String): String = value
        .replace(PACKAGE_SEPARATOR, " ")
        .replace(TEAM_FIELD_SEPARATOR, " ")
        .trim()

    private const val MINUTES_PER_DAY = 24 * 60

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
        this?.let { encoded -> enumValues<T>().firstOrNull { it.name == encoded } } ?: default
}
