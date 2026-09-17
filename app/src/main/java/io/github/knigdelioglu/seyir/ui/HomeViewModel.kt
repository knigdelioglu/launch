package io.github.knigdelioglu.seyir.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.knigdelioglu.seyir.data.AccentMode
import io.github.knigdelioglu.seyir.data.FavoriteTeam
import io.github.knigdelioglu.seyir.data.FootballApiException
import io.github.knigdelioglu.seyir.data.InstalledApp
import io.github.knigdelioglu.seyir.data.InstalledAppRepository
import io.github.knigdelioglu.seyir.data.InstalledAppSource
import io.github.knigdelioglu.seyir.data.LauncherPreferences
import io.github.knigdelioglu.seyir.data.LauncherPreferencesRepository
import io.github.knigdelioglu.seyir.data.LauncherPreferencesSource
import io.github.knigdelioglu.seyir.data.ThemeMode
import io.github.knigdelioglu.seyir.data.TodayMatch
import io.github.knigdelioglu.seyir.data.TodayMatchRepository
import io.github.knigdelioglu.seyir.data.TodayMatchSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

data class HomeUiState(
    val isLoading: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
    val hiddenApps: List<InstalledApp> = emptyList(),
    val favoriteApps: List<InstalledApp> = emptyList(),
    val favoritePackageNames: List<String> = emptyList(),
    val hiddenPackageNames: Set<String> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentMode: AccentMode = AccentMode.NEUTRAL,
    val reducedMotion: Boolean = false,
    val sportsApiConfigured: Boolean = false,
    val favoriteTeams: List<FavoriteTeam> = emptyList(),
    val todayMatches: List<TodayMatch> = emptyList(),
    val matchesLoading: Boolean = false,
    val matchesError: String? = null,
    val matchesFetchedAtMillis: Long = 0L,
    val teamSearchResults: List<FavoriteTeam> = emptyList(),
    val teamSearchLoading: Boolean = false,
    val teamSearchError: String? = null,
    val errorMessage: String? = null,
    val transientMessage: String? = null,
)

class HomeViewModel(
    application: Application,
    private val appRepository: InstalledAppSource,
    private val preferencesRepository: LauncherPreferencesSource,
    private val matchRepository: TodayMatchSource,
) : AndroidViewModel(application) {
    constructor(application: Application) : this(
        application = application,
        appRepository = InstalledAppRepository(application.applicationContext),
        preferencesRepository = LauncherPreferencesRepository(application.applicationContext),
        matchRepository = TodayMatchRepository(),
    )

    private var refreshJob: Job? = null
    private var matchesJob: Job? = null
    private var discoveredApps: List<InstalledApp> = appRepository.cachedLaunchableApps()
    private var latestPreferences = LauncherPreferences()
    private var lastAppRefreshCompletedAtMillis: Long = 0L

    private val _uiState = MutableStateFlow(
        HomeUiState(isLoading = discoveredApps.isEmpty()),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        if (discoveredApps.isNotEmpty()) {
            _uiState.value = renderPreferences(
                current = _uiState.value,
                preferences = latestPreferences,
                isLoading = false,
            )
        }
        observePreferences()
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return

        refreshJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = discoveredApps.isEmpty(),
                    errorMessage = null,
                )
            }

            try {
                val apps = appRepository.loadLaunchableApps()
                discoveredApps = apps

                val availablePackages = apps.mapTo(hashSetOf()) { it.packageName }
                preferencesRepository.cleanupUnavailablePackages(availablePackages)
                preferencesRepository.initializeFavoritesIfNeeded(
                    apps.map { it.packageName },
                )

                lastAppRefreshCompletedAtMillis = System.currentTimeMillis()
                _uiState.update { current ->
                    renderPreferences(
                        current = current,
                        preferences = latestPreferences,
                        isLoading = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Uygulamalar yüklenemedi.",
                    )
                }
            }
        }
    }

    fun refreshOnResume(nowMillis: Long = System.currentTimeMillis()) {
        if (
            shouldRefreshAppsOnResume(
                hasCachedApps = discoveredApps.isNotEmpty(),
                lastRefreshCompletedAtMillis = lastAppRefreshCompletedAtMillis,
                nowMillis = nowMillis,
            )
        ) {
            refresh()
        }
    }

    fun refreshTodayMatches(force: Boolean = false) {
        val today = LocalDate.now(TodayMatchRepository.TURKEY_TIME_ZONE)
        val todayValue = today.toString()
        when (decideMatchRefreshPlan(latestPreferences, todayValue, force)) {
            MatchRefreshPlan.DISABLED -> {
                _uiState.update {
                    it.copy(
                        sportsApiConfigured = false,
                        todayMatches = emptyList(),
                        matchesLoading = false,
                        matchesError = null,
                        matchesFetchedAtMillis = 0L,
                    )
                }
                return
            }

            MatchRefreshPlan.USE_CACHE -> {
                showCachedMatches(
                    preferences = latestPreferences,
                    favoriteTeamNames = latestPreferences.favoriteTeams.mapTo(linkedSetOf()) { it.name },
                )
                return
            }

            MatchRefreshPlan.SKIP_ALREADY_ATTEMPTED -> {
                _uiState.update {
                    it.copy(
                        matchesLoading = false,
                        matchesError = "Bugünkü maç sorgusu daha önce denendi. Yeniden denemek için sağdaki yenile butonuna basın.",
                    )
                }
                return
            }

            MatchRefreshPlan.FETCH -> Unit
        }

        val apiKey = latestPreferences.footballApiKey.trim()
        if (apiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    sportsApiConfigured = false,
                    todayMatches = emptyList(),
                    matchesLoading = false,
                    matchesError = null,
                    matchesFetchedAtMillis = 0L,
                )
            }
            return
        }
        if (matchesJob?.isActive == true) return

        val zoneId = TodayMatchRepository.TURKEY_TIME_ZONE
        val favoriteTeamNames = latestPreferences.favoriteTeams
            .mapTo(linkedSetOf()) { it.name }

        matchesJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    sportsApiConfigured = true,
                    matchesLoading = true,
                    matchesError = null,
                )
            }

            try {
                // Attempt date is persisted before the network call so process restarts cannot
                // accidentally create multiple API queries on the same calendar day.
                preferencesRepository.markDailyMatchAttempt(todayValue)

                val result = matchRepository.loadTodayMatches(
                    apiKey = apiKey,
                    zoneId = zoneId,
                    date = today,
                    favoriteTeamNames = favoriteTeamNames,
                )
                val fetchedAt = System.currentTimeMillis()
                preferencesRepository.saveDailyMatchCache(
                    date = todayValue,
                    json = result.rawJson,
                    fetchedAtMillis = fetchedAt,
                )

                _uiState.update {
                    it.copy(
                        todayMatches = result.matches,
                        matchesLoading = false,
                        matchesError = null,
                        matchesFetchedAtMillis = fetchedAt,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        todayMatches = emptyList(),
                        matchesLoading = false,
                        matchesError = matchErrorMessage(error),
                    )
                }
            }
        }
    }

    fun searchTeams(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < TEAM_SEARCH_MIN_LENGTH) {
            _uiState.update {
                it.copy(
                    teamSearchResults = emptyList(),
                    teamSearchLoading = false,
                    teamSearchError = "Takım eklemek için en az $TEAM_SEARCH_MIN_LENGTH karakter girin.",
                )
            }
            return
        }

        val team = FavoriteTeam(
            id = stableTeamId(normalizedQuery),
            name = normalizedQuery,
            country = "",
        )
        _uiState.update {
            it.copy(
                teamSearchResults = listOf(team),
                teamSearchLoading = false,
                teamSearchError = null,
            )
        }
    }

    fun clearTeamSearch() {
        _uiState.update {
            it.copy(
                teamSearchResults = emptyList(),
                teamSearchLoading = false,
                teamSearchError = null,
            )
        }
    }

    fun toggleFavoriteTeam(team: FavoriteTeam) {
        viewModelScope.launch {
            val selected = preferencesRepository.toggleFavoriteTeam(team)
            showMessage(
                if (selected) {
                    "${team.name} Takımlarım'a eklendi."
                } else {
                    "${team.name} Takımlarım'dan çıkarıldı."
                },
            )
        }
    }

    fun openApp(app: InstalledApp) {
        val launched = appRepository.launch(app.packageName)
        if (!launched) {
            showMessage("${app.label} açılamadı.")
        }
    }

    fun openAppInfo(app: InstalledApp) {
        if (!appRepository.openAppInfo(app.packageName)) {
            showMessage("${app.label} için uygulama bilgisi açılamadı.")
        }
    }

    fun toggleFavorite(app: InstalledApp) {
        viewModelScope.launch {
            val isFavorite = preferencesRepository.toggleFavorite(app.packageName)
            showMessage(
                if (isFavorite) {
                    "${app.label} favorilere eklendi."
                } else {
                    "${app.label} favorilerden çıkarıldı."
                },
            )
        }
    }

    fun moveFavorite(app: InstalledApp, offset: Int) {
        if (offset == 0) return

        viewModelScope.launch {
            val favorites = latestPreferences.favoritePackages.toMutableList()
            val currentIndex = favorites.indexOf(app.packageName)
            if (currentIndex == -1) return@launch

            val targetIndex = (currentIndex + offset).coerceIn(favorites.indices)
            if (targetIndex == currentIndex) return@launch

            val packageName = favorites.removeAt(currentIndex)
            favorites.add(targetIndex, packageName)
            preferencesRepository.setFavorites(favorites)
        }
    }

    fun setAppHidden(app: InstalledApp, hidden: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHidden(app.packageName, hidden)
            showMessage(
                if (hidden) {
                    "${app.label} gizlendi."
                } else {
                    "${app.label} yeniden gösteriliyor."
                },
            )
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(themeMode)
        }
    }

    fun setAccentMode(accentMode: AccentMode) {
        viewModelScope.launch {
            preferencesRepository.setAccentMode(accentMode)
        }
    }

    fun setReducedMotion(reducedMotion: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setReducedMotion(reducedMotion)
        }
    }

    fun setFootballApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesRepository.setFootballApiKey(apiKey)
            preferencesRepository.markDailyMatchAttempt("")
            if (apiKey.isBlank()) clearTeamSearch()
            showMessage(
                if (apiKey.isBlank()) {
                    "Bugün ne var devre dışı bırakıldı."
                } else {
                    "API-Football anahtarı kaydedildi."
                },
            )
        }
    }

    fun dismissTransientMessage() {
        _uiState.update { it.copy(transientMessage = null) }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(transientMessage = message) }
    }

    private fun showCachedMatches(
        preferences: LauncherPreferences,
        favoriteTeamNames: Set<String>,
    ) {
        val matches = matchRepository.parseCachedMatches(
            rawJson = preferences.dailyMatchCacheJson,
            favoriteTeamNames = favoriteTeamNames,
        )
        _uiState.update {
            it.copy(
                todayMatches = matches,
                matchesLoading = false,
                matchesError = null,
                matchesFetchedAtMillis = preferences.dailyMatchCacheFetchedAtMillis,
            )
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collectLatest { preferences ->
                val previousPreferences = latestPreferences
                val apiKeyChanged = preferences.footballApiKey != previousPreferences.footballApiKey
                val favoriteTeamsChanged = preferences.favoriteTeams != previousPreferences.favoriteTeams
                val cacheChanged =
                    preferences.dailyMatchCacheDate != previousPreferences.dailyMatchCacheDate ||
                        preferences.dailyMatchCacheJson != previousPreferences.dailyMatchCacheJson
                latestPreferences = preferences

                _uiState.update { current ->
                    renderPreferences(
                        current = current,
                        preferences = preferences,
                        isLoading = current.isLoading,
                    )
                }

                val today = LocalDate.now(TodayMatchRepository.TURKEY_TIME_ZONE)
                val favoriteNames = preferences.favoriteTeams.mapTo(linkedSetOf()) { it.name }
                if (
                    (favoriteTeamsChanged || cacheChanged) &&
                    preferences.dailyMatchCacheDate == today.toString() &&
                    preferences.dailyMatchCacheJson.isNotBlank()
                ) {
                    showCachedMatches(
                        preferences = preferences,
                        favoriteTeamNames = favoriteNames,
                    )
                }

                if (apiKeyChanged || preferences.footballApiKey.isNotBlank()) {
                    refreshTodayMatches(force = apiKeyChanged)
                }
            }
        }
    }

    private fun renderPreferences(
        current: HomeUiState,
        preferences: LauncherPreferences,
        isLoading: Boolean,
    ): HomeUiState {
        val packageState = deriveLauncherAppPackageState(
            availablePackageNames = discoveredApps.map { it.packageName },
            preferences = preferences,
        )
        val appsByPackage = discoveredApps.associateBy { it.packageName }
        val visibleApps = packageState.visiblePackageNames.mapNotNull(appsByPackage::get)
        val hiddenApps = packageState.hiddenPackageNames.mapNotNull(appsByPackage::get)
        val visibleAppByPackage = visibleApps.associateBy { it.packageName }

        val favoriteApps = if (preferences.favoritesInitialized) {
            packageState.favoritePackageNames.mapNotNull(visibleAppByPackage::get)
        } else {
            visibleApps.take(DEFAULT_FAVORITE_COUNT)
        }

        return current.copy(
            isLoading = isLoading,
            apps = visibleApps,
            hiddenApps = hiddenApps,
            favoriteApps = favoriteApps,
            favoritePackageNames = packageState.favoritePackageNames,
            hiddenPackageNames = packageState.hiddenPackageNames,
            themeMode = preferences.themeMode,
            accentMode = preferences.accentMode,
            reducedMotion = preferences.reducedMotion,
            sportsApiConfigured = preferences.footballApiKey.isNotBlank(),
            favoriteTeams = preferences.favoriteTeams,
            matchesFetchedAtMillis = preferences.dailyMatchCacheFetchedAtMillis,
            errorMessage = null,
        )
    }

    private fun stableTeamId(name: String): Int {
        val value = name.trim().lowercase(Locale.ROOT).hashCode() and Int.MAX_VALUE
        return if (value == 0) 1 else value
    }

    private fun matchErrorMessage(error: Exception): String = when (error) {
        is FootballApiException -> when (error.kind) {
            FootballApiException.Kind.UNAUTHORIZED -> "API-Football anahtarı geçersiz."
            FootballApiException.Kind.RATE_LIMITED -> "API-Football günlük istek kotası doldu."
            FootballApiException.Kind.NETWORK -> "Maç verisine ulaşılamadı; bağlantınızı kontrol edin."
            FootballApiException.Kind.INVALID_RESPONSE -> "Maç servisi geçersiz veri döndürdü."
            FootballApiException.Kind.UNKNOWN -> "Bugünün maçları alınamadı. Yarın yeniden denenecek."
        }

        else -> error.message ?: "Bugünün maçları alınamadı. Yarın yeniden denenecek."
    }

    private companion object {
        const val DEFAULT_FAVORITE_COUNT = 7
        const val TEAM_SEARCH_MIN_LENGTH = 3
    }
}
