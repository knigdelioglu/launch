package io.github.knigdelioglu.seyir.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.knigdelioglu.seyir.data.AccentMode
import io.github.knigdelioglu.seyir.data.FavoriteTeam
import io.github.knigdelioglu.seyir.data.InstalledApp
import io.github.knigdelioglu.seyir.data.InstalledAppRepository
import io.github.knigdelioglu.seyir.data.LauncherPreferences
import io.github.knigdelioglu.seyir.data.LauncherPreferencesRepository
import io.github.knigdelioglu.seyir.data.TeamSearchRepository
import io.github.knigdelioglu.seyir.data.ThemeMode
import io.github.knigdelioglu.seyir.data.TodayMatch
import io.github.knigdelioglu.seyir.data.TodayMatchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId


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
    val teamSearchResults: List<FavoriteTeam> = emptyList(),
    val teamSearchLoading: Boolean = false,
    val teamSearchError: String? = null,
    val errorMessage: String? = null,
    val transientMessage: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val appRepository = InstalledAppRepository(application.applicationContext)
    private val preferencesRepository = LauncherPreferencesRepository(application.applicationContext)
    private val matchRepository = TodayMatchRepository()
    private val teamSearchRepository = TeamSearchRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var matchesJob: Job? = null
    private var teamSearchJob: Job? = null
    private var discoveredApps: List<InstalledApp> = emptyList()
    private var latestPreferences = LauncherPreferences()
    private var lastMatchRefreshMillis = 0L
    private var lastMatchRefreshDate: LocalDate? = null

    init {
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

                _uiState.update { current ->
                    renderPreferences(
                        current = current,
                        preferences = latestPreferences,
                        isLoading = false,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Uygulamalar yüklenemedi.",
                    )
                }
            }
        }
    }

    fun refreshTodayMatches(force: Boolean = false) {
        val apiKey = latestPreferences.apiFootballKey.trim()
        if (apiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    sportsApiConfigured = false,
                    todayMatches = emptyList(),
                    matchesLoading = false,
                    matchesError = null,
                )
            }
            return
        }
        if (matchesJob?.isActive == true) return

        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val now = System.currentTimeMillis()
        val cacheFresh = lastMatchRefreshDate == today &&
            now - lastMatchRefreshMillis < MATCH_CACHE_MS
        if (!force && cacheFresh) return

        matchesJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    sportsApiConfigured = true,
                    matchesLoading = it.todayMatches.isEmpty(),
                    matchesError = null,
                )
            }

            try {
                val matches = matchRepository.loadTodayMatches(
                    apiKey = apiKey,
                    zoneId = zoneId,
                    date = today,
                    favoriteTeamIds = latestPreferences.favoriteTeams
                        .mapTo(hashSetOf()) { it.id },
                )
                lastMatchRefreshMillis = System.currentTimeMillis()
                lastMatchRefreshDate = today
                _uiState.update {
                    it.copy(
                        todayMatches = matches,
                        matchesLoading = false,
                        matchesError = null,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        matchesLoading = false,
                        matchesError = error.message ?: "Bugünün maçları alınamadı.",
                    )
                }
            }
        }
    }

    fun searchTeams(query: String) {
        val apiKey = latestPreferences.apiFootballKey.trim()
        if (apiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    teamSearchResults = emptyList(),
                    teamSearchLoading = false,
                    teamSearchError = "Önce API-Football anahtarını kaydedin.",
                )
            }
            return
        }

        val normalizedQuery = query.trim()
        if (normalizedQuery.length < TEAM_SEARCH_MIN_LENGTH) {
            _uiState.update {
                it.copy(
                    teamSearchResults = emptyList(),
                    teamSearchLoading = false,
                    teamSearchError = "Takım aramak için en az $TEAM_SEARCH_MIN_LENGTH karakter girin.",
                )
            }
            return
        }

        teamSearchJob?.cancel()
        teamSearchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    teamSearchLoading = true,
                    teamSearchError = null,
                )
            }

            try {
                val results = teamSearchRepository.searchTeams(
                    apiKey = apiKey,
                    query = normalizedQuery,
                )
                _uiState.update {
                    it.copy(
                        teamSearchResults = results,
                        teamSearchLoading = false,
                        teamSearchError = null,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        teamSearchResults = emptyList(),
                        teamSearchLoading = false,
                        teamSearchError = error.message ?: "Takım araması başarısız.",
                    )
                }
            }
        }
    }

    fun clearTeamSearch() {
        teamSearchJob?.cancel()
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

    fun setApiFootballKey(apiKey: String) {
        viewModelScope.launch {
            preferencesRepository.setApiFootballKey(apiKey)
            invalidateMatchCache()
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

    private fun invalidateMatchCache() {
        lastMatchRefreshMillis = 0L
        lastMatchRefreshDate = null
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collectLatest { preferences ->
                val previousPreferences = latestPreferences
                val apiKeyChanged = preferences.apiFootballKey != previousPreferences.apiFootballKey
                val favoriteTeamsChanged = preferences.favoriteTeams != previousPreferences.favoriteTeams
                latestPreferences = preferences

                _uiState.update { current ->
                    renderPreferences(
                        current = current,
                        preferences = preferences,
                        isLoading = current.isLoading,
                    )
                }

                if (apiKeyChanged || favoriteTeamsChanged) {
                    matchesJob?.cancel()
                    invalidateMatchCache()
                }

                if (preferences.apiFootballKey.isNotBlank()) {
                    refreshTodayMatches(force = apiKeyChanged || favoriteTeamsChanged)
                }
            }
        }
    }

    private fun renderPreferences(
        current: HomeUiState,
        preferences: LauncherPreferences,
        isLoading: Boolean,
    ): HomeUiState {
        val visibleApps = discoveredApps.filterNot {
            it.packageName in preferences.hiddenPackages
        }
        val hiddenApps = discoveredApps.filter {
            it.packageName in preferences.hiddenPackages
        }
        val visibleAppByPackage = visibleApps.associateBy { it.packageName }

        val favoriteApps = if (preferences.favoritesInitialized) {
            preferences.favoritePackages.mapNotNull(visibleAppByPackage::get)
        } else {
            visibleApps.take(DEFAULT_FAVORITE_COUNT)
        }

        return current.copy(
            isLoading = isLoading,
            apps = visibleApps,
            hiddenApps = hiddenApps,
            favoriteApps = favoriteApps,
            favoritePackageNames = preferences.favoritePackages,
            hiddenPackageNames = preferences.hiddenPackages,
            themeMode = preferences.themeMode,
            accentMode = preferences.accentMode,
            reducedMotion = preferences.reducedMotion,
            sportsApiConfigured = preferences.apiFootballKey.isNotBlank(),
            favoriteTeams = preferences.favoriteTeams,
            errorMessage = null,
        )
    }

    private companion object {
        const val DEFAULT_FAVORITE_COUNT = 7
        const val MATCH_CACHE_MS = 30L * 60L * 1_000L
        const val TEAM_SEARCH_MIN_LENGTH = 3
    }
}
