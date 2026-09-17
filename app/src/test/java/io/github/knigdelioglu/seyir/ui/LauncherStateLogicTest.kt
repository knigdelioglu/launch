package io.github.knigdelioglu.seyir.ui

import io.github.knigdelioglu.seyir.data.LauncherPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherStateLogicTest {
    @Test
    fun appPreferences_areTransformedIntoVisibleHiddenAndOrderedFavoriteState() {
        val state = deriveLauncherAppPackageState(
            availablePackageNames = listOf("a", "b", "c", "b"),
            preferences = LauncherPreferences(
                favoritesInitialized = true,
                favoritePackages = listOf("c", "removed", "a", "c"),
                hiddenPackages = setOf("b", "removed"),
            ),
        )

        assertEquals(listOf("a", "c"), state.visiblePackageNames)
        assertEquals(setOf("b"), state.hiddenPackageNames)
        assertEquals(listOf("c", "a"), state.favoritePackageNames)
    }

    @Test
    fun unavailableHiddenPackages_areNotReturned() {
        val state = deriveLauncherAppPackageState(
            availablePackageNames = listOf("available"),
            preferences = LauncherPreferences(hiddenPackages = setOf("missing")),
        )

        assertTrue(state.hiddenPackageNames.isEmpty())
        assertEquals(listOf("available"), state.visiblePackageNames)
    }
}
