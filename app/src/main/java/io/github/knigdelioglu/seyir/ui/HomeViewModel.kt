package io.github.knigdelioglu.seyir.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.knigdelioglu.seyir.data.InstalledApp
import io.github.knigdelioglu.seyir.data.InstalledAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class HomeUiState(
    val isLoading: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
    val errorMessage: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InstalledAppRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var hasLoaded = false

    init {
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isLoading && hasLoaded) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching { repository.loadLaunchableApps() }
                .onSuccess { apps ->
                    hasLoaded = true
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        apps = apps,
                    )
                }
                .onFailure { error ->
                    hasLoaded = true
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
        repository.launch(app.packageName)
    }
}
