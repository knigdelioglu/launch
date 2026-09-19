package io.github.knigdelioglu.seyir

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.knigdelioglu.seyir.ui.AllAppsScreen
import io.github.knigdelioglu.seyir.ui.AppearanceSettingsScreen
import io.github.knigdelioglu.seyir.ui.FavoriteAppsScreen
import io.github.knigdelioglu.seyir.ui.FavoriteTeamsScreen
import io.github.knigdelioglu.seyir.ui.HiddenAppsScreen
import io.github.knigdelioglu.seyir.ui.HomeScreen
import io.github.knigdelioglu.seyir.ui.HomeViewModel
import io.github.knigdelioglu.seyir.ui.SettingsScreen
import io.github.knigdelioglu.seyir.ui.SportsSettingsScreen
import io.github.knigdelioglu.seyir.ui.theme.SeyirTheme

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()
    private var packageReceiverRegistered = false

    private val packageChangesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterImmersiveMode()
        registerPackageReceiver()

        setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

            SeyirTheme(
                themeMode = uiState.themeMode,
                accentMode = uiState.accentMode,
                reducedMotion = uiState.reducedMotion,
            ) {
                var screen by rememberSaveable { mutableStateOf(SCREEN_HOME) }
                var homeFocusTarget by rememberSaveable { mutableStateOf<String?>(null) }
                var allAppsFocusTarget by rememberSaveable { mutableStateOf<String?>(null) }
                var favoriteAppsFocusTarget by rememberSaveable { mutableStateOf<String?>(null) }
                var allAppsReturnScreen by rememberSaveable { mutableStateOf(SCREEN_HOME) }

                BackHandler(enabled = screen != SCREEN_HOME) {
                    screen = when (screen) {
                        SCREEN_HIDDEN_APPS -> SCREEN_ALL_APPS
                        SCREEN_APPEARANCE_SETTINGS -> SCREEN_HOME
                        SCREEN_ALL_APPS -> allAppsReturnScreen
                        SCREEN_FAVORITE_APPS -> SCREEN_SETTINGS
                        SCREEN_FAVORITE_TEAMS -> {
                            viewModel.clearTeamSearch()
                            SCREEN_SPORTS_SETTINGS
                        }
                        SCREEN_SPORTS_SETTINGS -> SCREEN_SETTINGS
                        else -> SCREEN_HOME
                    }
                }

                when (screen) {
                    SCREEN_ALL_APPS -> AllAppsScreen(
                        apps = uiState.apps,
                        hiddenAppCount = uiState.hiddenApps.size,
                        favoritePackageNames = uiState.favoritePackageNames,
                        transientMessage = uiState.transientMessage,
                        focusTarget = allAppsFocusTarget,
                        onFocusTargetChanged = { allAppsFocusTarget = it },
                        onAppClick = viewModel::openApp,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onHideApp = { viewModel.setAppHidden(it, true) },
                        onOpenAppInfo = viewModel::openAppInfo,
                        onOpenHiddenApps = { screen = SCREEN_HIDDEN_APPS },
                        onBack = { screen = allAppsReturnScreen },
                        onDismissMessage = viewModel::dismissTransientMessage,
                    )

                    SCREEN_HIDDEN_APPS -> HiddenAppsScreen(
                        apps = uiState.hiddenApps,
                        transientMessage = uiState.transientMessage,
                        onRestore = { viewModel.setAppHidden(it, false) },
                        onBack = { screen = SCREEN_ALL_APPS },
                        onDismissMessage = viewModel::dismissTransientMessage,
                    )

                    SCREEN_SETTINGS -> SettingsScreen(
                        visibleAppCount = uiState.apps.size,
                        hiddenAppCount = uiState.hiddenApps.size,
                        favoriteAppCount = uiState.favoriteApps.size,
                        themeMode = uiState.manualThemeMode,
                        accentMode = uiState.accentMode,
                        reducedMotion = uiState.reducedMotion,
                        sportsApiConfigured = uiState.sportsApiConfigured,
                        onThemeModeChanged = viewModel::setThemeMode,
                        onAccentModeChanged = viewModel::setAccentMode,
                        onReducedMotionChanged = viewModel::setReducedMotion,
                        onOpenFavorites = {
                            favoriteAppsFocusTarget = null
                            screen = SCREEN_FAVORITE_APPS
                        },
                        onOpenApps = {
                            allAppsReturnScreen = SCREEN_SETTINGS
                            screen = SCREEN_ALL_APPS
                        },
                        onOpenSports = { screen = SCREEN_SPORTS_SETTINGS },
                        onBack = { screen = SCREEN_HOME },
                    )

                    SCREEN_FAVORITE_APPS -> FavoriteAppsScreen(
                        apps = uiState.apps,
                        favoritePackageNames = uiState.favoritePackageNames,
                        transientMessage = uiState.transientMessage,
                        focusTarget = favoriteAppsFocusTarget,
                        onFocusTargetChanged = { favoriteAppsFocusTarget = it },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onMoveFavorite = viewModel::moveFavorite,
                        onBack = { screen = SCREEN_SETTINGS },
                        onDismissMessage = viewModel::dismissTransientMessage,
                    )

                    SCREEN_SPORTS_SETTINGS -> SportsSettingsScreen(
                        configured = uiState.sportsApiConfigured,
                        favoriteTeamCount = uiState.favoriteTeams.size,
                        onSaveKey = viewModel::setFootballApiKey,
                        onClearKey = { viewModel.setFootballApiKey("") },
                        onOpenFavoriteTeams = {
                            viewModel.clearTeamSearch()
                            screen = SCREEN_FAVORITE_TEAMS
                        },
                        onBack = { screen = SCREEN_SETTINGS },
                    )

                    SCREEN_APPEARANCE_SETTINGS -> AppearanceSettingsScreen(
                        manualThemeMode = uiState.manualThemeMode,
                        scheduleEnabled = uiState.darkModeScheduleEnabled,
                        startMinutes = uiState.darkModeStartMinutes,
                        endMinutes = uiState.darkModeEndMinutes,
                        onManualDarkModeChanged = viewModel::setManualDarkMode,
                        onScheduleChanged = viewModel::setDarkModeSchedule,
                        onBack = { screen = SCREEN_HOME },
                    )

                    SCREEN_FAVORITE_TEAMS -> FavoriteTeamsScreen(
                        selectedTeams = uiState.favoriteTeams,
                        searchResults = uiState.teamSearchResults,
                        searchLoading = uiState.teamSearchLoading,
                        searchError = uiState.teamSearchError,
                        onSearch = viewModel::searchTeams,
                        onToggleTeam = viewModel::toggleFavoriteTeam,
                        onClearSearch = viewModel::clearTeamSearch,
                        onBack = {
                            viewModel.clearTeamSearch()
                            screen = SCREEN_SPORTS_SETTINGS
                        },
                    )

                    else -> HomeScreen(
                        uiState = uiState,
                        focusTarget = homeFocusTarget,
                        onFocusTargetChanged = { homeFocusTarget = it },
                        onAppClick = viewModel::openApp,
                        onMoveFavorite = viewModel::moveFavorite,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onOpenAllApps = {
                            allAppsReturnScreen = SCREEN_HOME
                            screen = SCREEN_ALL_APPS
                        },
                        onOpenSettings = { openAndroidSettings() },
                        onOpenAppearanceSettings = { screen = SCREEN_APPEARANCE_SETTINGS },
                        onRetry = viewModel::refresh,
                        onRefreshMatches = {
                            viewModel.refreshTodayMatches(force = true)
                            viewModel.refreshTodayMotorsport(force = true)
                        },
                        onDismissMessage = viewModel::dismissTransientMessage,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enterImmersiveMode()
        viewModel.refreshOnResume()
        viewModel.refreshTodayMatches()
        viewModel.refreshTodayMotorsport()
    }

    override fun onDestroy() {
        if (packageReceiverRegistered) {
            unregisterReceiver(packageChangesReceiver)
            packageReceiverRegistered = false
        }
        super.onDestroy()
    }

    private fun registerPackageReceiver() {
        if (packageReceiverRegistered) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        ContextCompat.registerReceiver(
            this,
            packageChangesReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
        packageReceiverRegistered = true
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun openAndroidSettings() {
        startActivity(Intent(AndroidSettings.ACTION_SETTINGS))
    }

    private companion object {
        const val SCREEN_HOME = "home"
        const val SCREEN_ALL_APPS = "all_apps"
        const val SCREEN_HIDDEN_APPS = "hidden_apps"
        const val SCREEN_APPEARANCE_SETTINGS = "appearance_settings"
        const val SCREEN_SETTINGS = "settings"
        const val SCREEN_FAVORITE_APPS = "favorite_apps"
        const val SCREEN_SPORTS_SETTINGS = "sports_settings"
        const val SCREEN_FAVORITE_TEAMS = "favorite_teams"
    }
}
