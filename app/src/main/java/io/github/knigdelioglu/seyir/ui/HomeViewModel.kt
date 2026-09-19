package io.github.knigdelioglu.seyir.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.knigdelioglu.seyir.data.AccentMode
import io.github.knigdelioglu.seyir.data.DEFAULT_DARK_MODE_END_MINUTES
import io.github.knigdelioglu.seyir.data.DEFAULT_DARK_MODE_START_MINUTES
import io.github.knigdelioglu.seyir.data.FavoriteTeam
import io.github.knigdelioglu.seyir.data.FootballApiException
import io.github.knigdelioglu.seyir.data.InstalledApp
import io.github.knigdelioglu.seyir.data.InstalledAppRepository
import io.github.knigdelioglu.seyir.data.InstalledAppSource
import io.github.knigdelioglu.seyir.data.LauncherPreferences
import io.github.knigdelioglu.seyir.data.LauncherPreferencesRepository
import io.github.knigdelioglu.seyir.data.LauncherPreferencesSource
import io.github.knigdelioglu.seyir.data.MotorsportRepository
import io.github.knigdelioglu.seyir.data.MotorsportSource
import io.github.knigdelioglu.seyir.data.ThemeMode
import io.github.knigdelioglu.seyir.data.TodayMotorsportSession
import io.github.knigdelioglu.seyir.data.TodayMatch
import io.github.knigdelioglu.seyir.data.TodayMatchRepository
import io.github.knigdelioglu.seyir.data.TodayMatchSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

data class HomeUiState(
    val isLoading: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
    val hiddenApps: List<InstalledApp> = emptyList(),
    val favoriteApps: List<InstalledApp> = emptyList(),
    val favoritePackageNames: List<String> = emptyList(),
    val hiddenPackageNames: Set<String> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    val manualThemeMode: ThemeMode = ThemeMode.DARK,
    val darkModeScheduleEnabled: Boolean = false,
    val darkModeStartMinutes: Int = DEFAULT_DARK_MODE_START_MINUTES,
    val darkModeEndMinutes: Int = DEFAULT_DARK_MODE_END_MINUTES,
    val accentMode: AccentMode = AccentMode.NEUTRAL,
    val reducedMotion: Boolean = false,
    val sportsApiConfigured: Boolean = false,
    val favoriteTeams: List<FavoriteTeam> = emptyList(),
    val todayMatches: List<TodayMatch> = emptyList(),
    val matchesLoading: Boolean = false,
    val matchesError: String? = null,
    val matchesFetchedAtMillis: Long = 0L,
    val motorsportSessions: List<TodayMotorsportSession> = emptyList(),
    val motorsportLoading: Boolean = false,
    val motorsportError: String? = null,
    val motorsportFetchedAtMillis: Long = 0L,
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
    private val motorsportRepository: MotorsportSource,
) : AndroidViewModel(application) {
    constructor(application: Application) : this(
        application = application,
        appRepository = InstalledAppRepository(application.applicationContext),
        preferencesRepository = LauncherPreferencesRepository(application.applicationContext),
        matchRepository = TodayMatchRepository(),
        motorsportRepository = MotorsportRepository(),
    )

    private var refreshJob: Job? = null
    private var matchesJob: Job? = null
    private var cachedMatchesJob: Job? = null
    private var motorsportJob: Job? = null
    private var cachedMotorsportJob: Job? = null
    private var discoveredApps: List<InstalledApp> = appRepository.cachedLaunchableApps()
    private var latestPreferences = LauncherPreferences()
    private var hasObservedPreferences = false
    private var renderedMatchCacheKey: MatchCacheKey? = null
    private var renderedMotorsportCacheKey: MotorsportCacheKey? = null

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
        observeThemeScheduleClock()
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

    fun refreshOnResume() {
        if (shouldRefreshAppsOnResume(hasCachedApps = discoveredApps.isNotEmpty())) {
            refresh()
        }
    }

    fun refreshTodayMatches(force: Boolean = false) {
        if (!hasObservedPreferences && !force) return

        val today = LocalDate.now(TodayMatchRepository.TURKEY_TIME_ZONE)
        val todayValue = today.toString()
        when (decideMatchRefreshPlan(latestPreferences, todayValue, force)) {
            MatchRefreshPlan.DISABLED -> {
                cachedMatchesJob?.cancel()
                cachedMatchesJob = null
                renderedMatchCacheKey = null
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
                val preferences = latestPreferences
                val favoriteTeamNames = preferences.favoriteTeams.mapTo(linkedSetOf()) { it.name }
                val currentMatches = _uiState.value.todayMatches
                val currentKey = renderedMatchCacheKey
                if (
                    currentMatches.isNotEmpty() &&
                    currentKey?.date == preferences.dailyMatchCacheDate &&
                    currentKey.rawJson == preferences.dailyMatchCacheJson &&
                    currentKey.favoriteTeamNames == favoriteTeamNames
                ) {
                    return
                }

                if (cachedMatchesJob?.isActive != true) {
                    cachedMatchesJob = viewModelScope.launch {
                        showCachedMatches(
                            preferences = preferences,
                            favoriteTeamNames = favoriteTeamNames,
                        )
                    }
                }
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

        cachedMatchesJob?.cancel()
        cachedMatchesJob = null
        renderedMatchCacheKey = null
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
                renderedMatchCacheKey = MatchCacheKey(
                    date = todayValue,
                    rawJson = result.rawJson,
                    favoriteTeamNames = favoriteTeamNames.toSet(),
                )
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

    fun refreshTodayMotorsport(force: Boolean = false) {
        if (!hasObservedPreferences && !force) return

        val zoneId = TodayMatchRepository.TURKEY_TIME_ZONE
        val today = LocalDate.now(zoneId)
        val todayValue = today.toString()
        val preferences = latestPreferences
        val hasTodayCache =
            preferences.dailyMotorsportCacheDate == todayValue &&
                preferences.dailyMotorsportCacheJson.isNotBlank()

        if (!force && hasTodayCache) {
            val cacheKey = MotorsportCacheKey(
                date = preferences.dailyMotorsportCacheDate,
                rawJson = preferences.dailyMotorsportCacheJson,
            )
            if (cacheKey == renderedMotorsportCacheKey) return

            if (cachedMotorsportJob?.isActive != true) {
                cachedMotorsportJob = viewModelScope.launch {
                    showCachedMotorsport(preferences)
                }
            }
            return
        }

        if (!force && preferences.dailyMotorsportAttemptDate == todayValue) {
            _uiState.update {
                it.copy(
                    motorsportLoading = false,
                    motorsportError = null,
                )
            }
            return
        }
        if (motorsportJob?.isActive == true) return

        cachedMotorsportJob?.cancel()
        cachedMotorsportJob = null
        renderedMotorsportCacheKey = null
        motorsportJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    motorsportLoading = true,
                    motorsportError = null,
                )
            }

            try {
                preferencesRepository.markDailyMotorsportAttempt(todayValue)
                val result = motorsportRepository.loadTodaySessions(
                    zoneId = zoneId,
                    date = today,
                )
                val fetchedAt = System.currentTimeMillis()
                renderedMotorsportCacheKey = MotorsportCacheKey(
                    date = todayValue,
                    rawJson = result.rawJson,
                )
                preferencesRepository.saveDailyMotorsportCache(
                    date = todayValue,
                    json = result.rawJson,
                    fetchedAtMillis = fetchedAt,
                )
                _uiState.update {
                    it.copy(
                        motorsportSessions = result.sessions,
                        motorsportLoading = false,
                        motorsportError = null,
                        motorsportFetchedAtMillis = fetchedAt,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        motorsportLoading = false,
                        motorsportError = "F1 / MotoGP verisi alınamadı.",
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

    fun setManualDarkMode(enabled: Boolean) {
        setThemeMode(if (enabled) ThemeMode.BLACK else ThemeMode.DARK)
    }

    fun setDarkModeSchedule(
        enabled: Boolean,
        startMinutes: Int,
        endMinutes: Int,
    ) {
        viewModelScope.launch {
            preferencesRepository.setDarkModeSchedule(
                enabled = enabled,
                startMinutes = startMinutes,
                endMinutes = endMinutes,
            )
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

    private suspend fun showCachedMatches(
        preferences: LauncherPreferences,
        favoriteTeamNames: Set<String>,
    ) {
        val cacheKey = MatchCacheKey(
            date = preferences.dailyMatchCacheDate,
            rawJson = preferences.dailyMatchCacheJson,
            favoriteTeamNames = favoriteTeamNames.toSet(),
        )
        if (cacheKey == renderedMatchCacheKey && _uiState.value.todayMatches.isNotEmpty()) return

        val matches = matchRepository.parseCachedMatches(
            rawJson = preferences.dailyMatchCacheJson,
            favoriteTeamNames = favoriteTeamNames,
        )
        renderedMatchCacheKey = cacheKey
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
                val isInitialEmission = !hasObservedPreferences
                val apiKeyChanged = preferences.footballApiKey != previousPreferences.footballApiKey
                val favoriteTeamsChanged = preferences.favoriteTeams != previousPreferences.favoriteTeams
                val cacheChanged =
                    preferences.dailyMatchCacheDate != previousPreferences.dailyMatchCacheDate ||
                        preferences.dailyMatchCacheJson != previousPreferences.dailyMatchCacheJson
                latestPreferences = preferences
                hasObservedPreferences = true

                _uiState.update { current ->
                    renderPreferences(
                        current = current,
                        preferences = preferences,
                        isLoading = current.isLoading,
                    )
                }

                val today = LocalDate.now(TodayMatchRepository.TURKEY_TIME_ZONE)
                val todayValue = today.toString()
                val favoriteNames = preferences.favoriteTeams.mapTo(linkedSetOf()) { it.name }
                val hasTodayCache =
                    preferences.dailyMatchCacheDate == todayValue &&
                        preferences.dailyMatchCacheJson.isNotBlank()

                if (hasTodayCache && (favoriteTeamsChanged || cacheChanged || _uiState.value.todayMatches.isEmpty())) {
                    showCachedMatches(
                        preferences = preferences,
                        favoriteTeamNames = favoriteNames,
                    )
                }

                when {
                    apiKeyChanged && !isInitialEmission -> refreshTodayMatches(force = true)
                    preferences.footballApiKey.isNotBlank() && !hasTodayCache -> refreshTodayMatches()
                }
            }
        }
    }

    private fun observeThemeScheduleClock() {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                if (!latestPreferences.darkModeScheduleEnabled) continue

                val effectiveTheme = effectiveThemeMode(latestPreferences)
                _uiState.update { current ->
                    if (current.themeMode == effectiveTheme) current else current.copy(themeMode = effectiveTheme)
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
            themeMode = effectiveThemeMode(preferences),
            manualThemeMode = preferences.themeMode,
            darkModeScheduleEnabled = preferences.darkModeScheduleEnabled,
            darkModeStartMinutes = preferences.darkModeStartMinutes,
            darkModeEndMinutes = preferences.darkModeEndMinutes,
            accentMode = preferences.accentMode,
            reducedMotion = preferences.reducedMotion,
            sportsApiConfigured = preferences.footballApiKey.isNotBlank(),
            favoriteTeams = preferences.favoriteTeams,
            matchesFetchedAtMillis = preferences.dailyMatchCacheFetchedAtMillis,
            errorMessage = null,
        )
    }

    private fun effectiveThemeMode(preferences: LauncherPreferences): ThemeMode {
        val now = LocalTime.now()
        return resolveThemeMode(
            manualThemeMode = preferences.themeMode,
            scheduleEnabled = preferences.darkModeScheduleEnabled,
            startMinutes = preferences.darkModeStartMinutes,
            endMinutes = preferences.darkModeEndMinutes,
            nowMinutes = now.hour * 60 + now.minute,
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

    private data class MatchCacheKey(
        val date: String,
        val rawJson: String,
        val favoriteTeamNames: Set<String>,
    )

    private companion object {
        const val DEFAULT_FAVORITE_COUNT = 7
        const val TEAM_SEARCH_MIN_LENGTH = 3
    }
}
