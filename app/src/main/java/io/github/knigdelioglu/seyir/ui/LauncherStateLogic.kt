package io.github.knigdelioglu.seyir.ui

import io.github.knigdelioglu.seyir.data.LauncherPreferences

internal data class LauncherAppPackageState(
    val visiblePackageNames: List<String>,
    val hiddenPackageNames: Set<String>,
    val favoritePackageNames: List<String>,
)

internal fun deriveLauncherAppPackageState(
    availablePackageNames: List<String>,
    preferences: LauncherPreferences,
): LauncherAppPackageState {
    val available = availablePackageNames
        .asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toList()
        .distinct()
    val availableSet = available.toSet()
    val hidden = preferences.hiddenPackages.filterTo(linkedSetOf()) { it in availableSet }
    val favorites = preferences.favoritePackages
        .filter { it in availableSet }
        .distinct()

    return LauncherAppPackageState(
        visiblePackageNames = available.filterNot { it in hidden },
        hiddenPackageNames = hidden,
        favoritePackageNames = favorites,
    )
}
