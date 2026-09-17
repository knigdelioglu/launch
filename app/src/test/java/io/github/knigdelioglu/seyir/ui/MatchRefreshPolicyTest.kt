package io.github.knigdelioglu.seyir.ui

import io.github.knigdelioglu.seyir.data.LauncherPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class MatchRefreshPolicyTest {
    private val today = "2026-09-17"

    @Test
    fun cacheOnSameDay_preventsSecondApiCall() {
        val preferences = LauncherPreferences(
            footballApiKey = "key",
            dailyMatchCacheDate = today,
            dailyMatchCacheJson = "{}",
        )

        assertEquals(MatchRefreshPlan.USE_CACHE, decideMatchRefreshPlan(preferences, today, force = false))
    }

    @Test
    fun failedAttemptOnSameDay_isNotRetriedAutomatically() {
        val preferences = LauncherPreferences(
            footballApiKey = "key",
            dailyMatchAttemptDate = today,
        )

        assertEquals(
            MatchRefreshPlan.SKIP_ALREADY_ATTEMPTED,
            decideMatchRefreshPlan(preferences, today, force = false),
        )
    }

    @Test
    fun forceRefresh_ignoresCacheAndAttemptDate() {
        val preferences = LauncherPreferences(
            footballApiKey = "key",
            dailyMatchAttemptDate = today,
            dailyMatchCacheDate = today,
            dailyMatchCacheJson = "{}",
        )

        assertEquals(MatchRefreshPlan.FETCH, decideMatchRefreshPlan(preferences, today, force = true))
    }

    @Test
    fun missingApiKey_disablesSportsRefresh() {
        assertEquals(
            MatchRefreshPlan.DISABLED,
            decideMatchRefreshPlan(
                LauncherPreferences(footballApiKey = ""),
                today,
                force = false,
            ),
        )
    }
}
