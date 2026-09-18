package io.github.knigdelioglu.seyir.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResumeRefreshPolicyTest {
    @Test
    fun emptySnapshot_refreshesOnResume() {
        assertTrue(
            shouldRefreshAppsOnResume(
                hasCachedApps = false,
            ),
        )
    }

    @Test
    fun cachedSnapshot_skipsPeriodicResumeRescan() {
        assertFalse(
            shouldRefreshAppsOnResume(
                hasCachedApps = true,
            ),
        )
    }
}
