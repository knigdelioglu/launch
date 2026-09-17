package io.github.knigdelioglu.seyir.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

interface InstalledAppSource {
    suspend fun loadLaunchableApps(): List<InstalledApp>

    fun launch(packageName: String): Boolean

    fun openAppInfo(packageName: String): Boolean
}

interface LauncherPreferencesSource {
    val preferences: Flow<LauncherPreferences>

    suspend fun initializeFavoritesIfNeeded(packageNames: List<String>)

    suspend fun setFavorites(packageNames: List<String>)

    suspend fun toggleFavorite(packageName: String): Boolean

    suspend fun setHidden(packageName: String, hidden: Boolean)

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setAccentMode(accentMode: AccentMode)

    suspend fun setReducedMotion(reducedMotion: Boolean)

    suspend fun setFootballApiKey(apiKey: String)

    suspend fun markDailyMatchAttempt(date: String)

    suspend fun saveDailyMatchCache(date: String, json: String, fetchedAtMillis: Long)

    suspend fun toggleFavoriteTeam(team: FavoriteTeam): Boolean

    suspend fun cleanupUnavailablePackages(availablePackages: Set<String>)
}

interface TodayMatchSource {
    suspend fun loadTodayMatches(
        apiKey: String,
        zoneId: ZoneId,
        date: LocalDate,
        favoriteTeamNames: Set<String> = emptySet(),
    ): DailyMatchResult

    fun parseCachedMatches(
        rawJson: String,
        favoriteTeamNames: Set<String> = emptySet(),
    ): List<TodayMatch>
}
