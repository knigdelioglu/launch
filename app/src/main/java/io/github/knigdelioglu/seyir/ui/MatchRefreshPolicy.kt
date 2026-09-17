package io.github.knigdelioglu.seyir.ui

import io.github.knigdelioglu.seyir.data.LauncherPreferences

internal enum class MatchRefreshPlan {
    DISABLED,
    USE_CACHE,
    SKIP_ALREADY_ATTEMPTED,
    FETCH,
}

internal fun decideMatchRefreshPlan(
    preferences: LauncherPreferences,
    today: String,
    force: Boolean,
): MatchRefreshPlan {
    if (preferences.footballApiKey.isBlank()) return MatchRefreshPlan.DISABLED
    if (force) return MatchRefreshPlan.FETCH
    if (
        preferences.dailyMatchCacheDate == today &&
        preferences.dailyMatchCacheJson.isNotBlank()
    ) {
        return MatchRefreshPlan.USE_CACHE
    }
    if (preferences.dailyMatchAttemptDate == today) {
        return MatchRefreshPlan.SKIP_ALREADY_ATTEMPTED
    }
    return MatchRefreshPlan.FETCH
}
