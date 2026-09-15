package io.github.knigdelioglu.seyir.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.launcherPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "launcher_preferences",
)

data class LauncherPreferences(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val favoritesInitialized: Boolean = false,
    val favoritePackages: List<String> = emptyList(),
    val hiddenPackages: Set<String> = emptySet(),
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
    )

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

    private companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val DEFAULT_FAVORITE_COUNT = 7
        const val PACKAGE_SEPARATOR = "\n"

        val SCHEMA_VERSION = intPreferencesKey("schema_version")
        val FAVORITES_INITIALIZED = booleanPreferencesKey("favorites_initialized")
        val FAVORITE_PACKAGES = stringPreferencesKey("favorite_packages_v1")
        val HIDDEN_PACKAGES = stringSetPreferencesKey("hidden_packages_v1")
    }
}

private const val CURRENT_SCHEMA_VERSION = 1
