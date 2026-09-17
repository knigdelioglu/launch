package io.github.knigdelioglu.seyir.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResumeRefreshPolicyTest {
    @Test
    fun emptySnapshot_alwaysRefreshes() {
        assertTrue(
            shouldRefreshAppsOnResume(
                hasCachedApps = false,
                lastRefreshCompletedAtMillis = 10_000L,
                nowMillis = 10_001L,
            ),
        )
    }

    @Test
    fun recentRefresh_withCachedApps_skipsRescan() {
        assertFalse(
            shouldRefreshAppsOnResume(
                hasCachedApps = true,
                lastRefreshCompletedAtMillis = 10_000L,
                nowMillis = 10_000L + APP_RESUME_REFRESH_INTERVAL_MILLIS - 1,
            ),
        )
    }

    @Test
    fun staleRefresh_withCachedApps_runsAgain() {
        assertTrue(
            shouldRefreshAppsOnResume(
                hasCachedApps = true,
                lastRefreshCompletedAtMillis = 10_000L,
                nowMillis = 10_000L + APP_RESUME_REFRESH_INTERVAL_MILLIS,
            ),
        )
    }
}
