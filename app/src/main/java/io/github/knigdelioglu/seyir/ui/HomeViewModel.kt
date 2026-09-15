package io.github.knigdelioglu.seyir.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.knigdelioglu.seyir.data.InstalledApp
import io.github.knigdelioglu.seyir.data.InstalledAppRepository
import io.github.knigdelioglu.seyir.data.LauncherPreferences
import io.github.knigdelioglu.seyir.data.LauncherPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class HomeUiState(
    val isLoading: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
    val favoriteApps: List<InstalledApp> = emptyList(),
    val favoritePackageNames: List<String> = emptyList(),
    val hiddenPackageNames: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val transientMessage: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val appRepository = InstalledAppRepository(application.applicationContext)
    private val preferencesRepository = LauncherPreferencesRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var discoveredApps: List<InstalledApp> = emptyList()
    private var latestPreferences = LauncherPreferences()

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

    fun openApp(app: InstalledApp) {
        val launched = appRepository.launch(app.packageName)
        if (!launched) {
            _uiState.update {
                it.copy(transientMessage = "${app.label} açılamadı.")
            }
        }
    }

    fun toggleFavorite(app: InstalledApp) {
        viewModelScope.launch {
            val isFavorite = preferencesRepository.toggleFavorite(app.packageName)
            _uiState.update {
                it.copy(
                    transientMessage = if (isFavorite) {
                        "${app.label} favorilere eklendi."
                    } else {
                        "${app.label} favorilerden çıkarıldı."
                    },
                )
            }
        }
    }

    fun setAppHidden(app: InstalledApp, hidden: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHidden(app.packageName, hidden)
            _uiState.update {
                it.copy(
                    transientMessage = if (hidden) {
                        "${app.label} gizlendi."
                    } else {
                        "${app.label} yeniden gösteriliyor."
                    },
                )
            }
        }
    }

    fun dismissTransientMessage() {
        _uiState.update { it.copy(transientMessage = null) }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collectLatest { preferences ->
                latestPreferences = preferences
                _uiState.update { current ->
                    renderPreferences(
                        current = current,
                        preferences = preferences,
                        isLoading = current.isLoading,
                    )
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
        val appByPackage = visibleApps.associateBy { it.packageName }

        val favoriteApps = if (preferences.favoritesInitialized) {
            preferences.favoritePackages.mapNotNull(appByPackage::get)
        } else {
            visibleApps.take(DEFAULT_FAVORITE_COUNT)
        }

        return current.copy(
            isLoading = isLoading,
            apps = visibleApps,
            favoriteApps = favoriteApps,
            favoritePackageNames = preferences.favoritePackages,
            hiddenPackageNames = preferences.hiddenPackages,
            errorMessage = null,
        )
    }

    private companion object {
        const val DEFAULT_FAVORITE_COUNT = 7
    }
}
