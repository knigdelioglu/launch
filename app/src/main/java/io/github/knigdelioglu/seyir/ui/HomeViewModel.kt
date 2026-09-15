package io.github.knigdelioglu.seyir.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.knigdelioglu.seyir.data.InstalledApp
import io.github.knigdelioglu.seyir.data.InstalledAppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class HomeUiState(
    val isLoading: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
    val errorMessage: String? = null,
    val transientMessage: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InstalledAppRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return

        refreshJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = it.apps.isEmpty(),
                    errorMessage = null,
                )
            }

            runCatching { repository.loadLaunchableApps() }
                .onSuccess { apps ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            apps = apps,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
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
        val launched = repository.launch(app.packageName)
        if (!launched) {
            _uiState.update {
                it.copy(transientMessage = "${app.label} açılamadı.")
            }
        }
    }

    fun dismissTransientMessage() {
        _uiState.update { it.copy(transientMessage = null) }
    }
}
