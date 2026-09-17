package io.github.knigdelioglu.seyir.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CancellationException

internal enum class FocusRestoreReason {
    SCREEN_ENTRY,
    DIALOG_DISMISSED,
    DATASET_CHANGED,
    ITEM_REMOVED,
    ORDER_CHANGED,
}

internal data class FocusRestoreRequest(
    val version: Int = 0,
    val reason: FocusRestoreReason = FocusRestoreReason.SCREEN_ENTRY,
) {
    fun next(reason: FocusRestoreReason): FocusRestoreRequest = copy(
        version = version + 1,
        reason = reason,
    )
}

internal data class FocusRestoreKey(
    val itemKeys: List<String>,
    val requestVersion: Int,
)

internal fun shouldStartFocusRestore(
    previous: FocusRestoreKey?,
    current: FocusRestoreKey,
): Boolean = previous == null || previous != current

internal fun resolveFocusTargetIndex(
    itemKeys: List<String>,
    focusTarget: String?,
): Int? {
    if (itemKeys.isEmpty()) return null
    return itemKeys.indexOf(focusTarget).takeIf { it >= 0 } ?: 0
}

internal fun adjacentFallbackKey(
    itemKeys: List<String>,
    removedKey: String,
): String? {
    val removedIndex = itemKeys.indexOf(removedKey)
    if (removedIndex < 0) return null
    return itemKeys.getOrNull(removedIndex + 1)
        ?: itemKeys.getOrNull(removedIndex - 1)
}

/**
 * The effect is intentionally keyed only by the dataset identity and an
 * explicit request version. Focus history is read as the latest value but is
 * never an effect key, so ordinary D-pad movement cannot start a restore.
 */
@Composable
internal fun FocusRestoreEffect(
    itemKeys: List<String>,
    focusTarget: String?,
    request: FocusRestoreRequest,
    onRestore: suspend (String?) -> Unit,
) {
    val latestFocusTarget by rememberUpdatedState(focusTarget)
    val latestOnRestore by rememberUpdatedState(onRestore)

    LaunchedEffect(itemKeys, request.version) {
        awaitFocusLayout()
        latestOnRestore(latestFocusTarget)
    }
}

internal suspend fun awaitFocusLayout() {
    withFrameNanos { }
}

internal suspend fun restoreItemFocus(
    itemKeys: List<String>,
    focusTarget: String?,
    isItemVisible: (Int) -> Boolean,
    scrollToItem: suspend (Int) -> Unit,
    requestFocus: (String) -> Unit,
): Boolean {
    val targetIndex = resolveFocusTargetIndex(itemKeys, focusTarget) ?: return false
    if (!isItemVisible(targetIndex)) {
        try {
            scrollToItem(targetIndex)
        } catch (error: CancellationException) {
            throw error
        } catch (_: IllegalArgumentException) {
            return false
        } catch (_: IllegalStateException) {
            return false
        }
        awaitFocusLayout()
    }
    requestFocusBestEffort { requestFocus(itemKeys[targetIndex]) }
    return true
}

internal inline fun requestFocusBestEffort(request: () -> Unit) {
    try {
        request()
    } catch (error: CancellationException) {
        throw error
    } catch (_: IllegalStateException) {
        // FocusRequester can be temporarily unattached during a list update.
    }
}
