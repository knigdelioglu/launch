package io.github.knigdelioglu.seyir.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPreferencesRepositoryTest {
    @Test
    fun favorites_addRemoveAndOrder_areDeduplicated() = runBlocking {
        val repository = repository()

        repository.setFavorites(listOf(" a ", "com.example.b", "a", ""))
        assertEquals(listOf("a", "com.example.b"), repository.preferences.first().favoritePackages)

        assertFalse(repository.toggleFavorite("a"))
        assertEquals(listOf("com.example.b"), repository.preferences.first().favoritePackages)

        assertTrue(repository.toggleFavorite("com.example.c"))
        assertEquals(
            listOf("com.example.b", "com.example.c"),
            repository.preferences.first().favoritePackages,
        )
    }

    @Test
    fun hiddenApps_andUnavailablePackages_areCleanedTogether() = runBlocking {
        val repository = repository()

        repository.setFavorites(listOf("keep", "removed"))
        repository.setHidden("keep", hidden = true)
        repository.setHidden("removed", hidden = true)
        repository.cleanupUnavailablePackages(setOf("keep"))

        val preferences = repository.preferences.first()
        assertEquals(listOf("keep"), preferences.favoritePackages)
        assertEquals(setOf("keep"), preferences.hiddenPackages)
    }

    @Test
    fun blankStoredApiKey_overridesBuildTimeFallback() = runBlocking {
        val repository = repository(defaultApiKey = "build-time-key")

        repository.setFootballApiKey("")

        assertEquals("", repository.preferences.first().footballApiKey)
    }

    @Test
    fun legacySchema_isMigratedWithoutLosingKnownValues() = runBlocking {
        val dataStore = MemoryPreferencesDataStore()
        dataStore.updateData { current ->
            current.toMutablePreferences().apply {
                this[intPreferencesKey("schema_version")] = 2
                this[stringPreferencesKey("favorite_packages_v1")] = "first\nfirst\nsecond"
            }
        }
        val repository = LauncherPreferencesRepository(dataStore, defaultApiKey = "")

        val preferences = repository.preferences.first()

        assertEquals(CURRENT_LAUNCHER_SCHEMA_VERSION, preferences.schemaVersion)
        assertEquals(listOf("first", "second"), preferences.favoritePackages)
    }

    @Test
    fun malformedEnumsAndTeamRecords_useSafeDefaults() {
        val raw = emptyPreferences().toMutablePreferences().apply {
            this[stringPreferencesKey("theme_mode_v1")] = "NOT_A_THEME"
            this[stringPreferencesKey("accent_mode_v1")] = "UNKNOWN"
            this[stringPreferencesKey("favorite_teams_v1")] =
                "645\u001FGalatasaray\u001FTürkiye\n" +
                    "not-a-team\n" +
                    "611\u001F\u001F\n" +
                    "611\u001FFenerbahçe\u001FTürkiye"
        }

        val preferences = LauncherPreferencesCodec.decode(raw, defaultApiKey = "")

        assertEquals(ThemeMode.DARK, preferences.themeMode)
        assertEquals(AccentMode.NEUTRAL, preferences.accentMode)
        assertEquals(
            listOf(
                FavoriteTeam(645, "Galatasaray", "Türkiye"),
                FavoriteTeam(611, "Fenerbahçe", "Türkiye"),
            ),
            preferences.favoriteTeams,
        )
    }

    @Test
    fun favoriteTeamCodec_sanitizesDelimiters_andRoundTrips() {
        val encoded = LauncherPreferencesCodec.encodeFavoriteTeams(
            listOf(
                FavoriteTeam(645, "Galatasaray\nKadro", "TR\u001F"),
                FavoriteTeam(645, "duplicate", "ignored"),
            ),
        )

        assertEquals(
            listOf(FavoriteTeam(645, "Galatasaray Kadro", "TR")),
            LauncherPreferencesCodec.decodeFavoriteTeams(encoded),
        )
    }

    @Test
    fun favoriteTeamToggle_addsAndRemovesByStableId() = runBlocking {
        val repository = repository()
        val team = FavoriteTeam(645, "Galatasaray", "Türkiye")

        assertTrue(repository.toggleFavoriteTeam(team))
        assertFalse(repository.toggleFavoriteTeam(team.copy(name = "Different label")))
        assertTrue(repository.preferences.first().favoriteTeams.isEmpty())
    }

    private fun repository(defaultApiKey: String = ""): LauncherPreferencesRepository =
        LauncherPreferencesRepository(
            dataStore = MemoryPreferencesDataStore(),
            defaultApiKey = defaultApiKey,
        )
}

private class MemoryPreferencesDataStore(
    initial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
