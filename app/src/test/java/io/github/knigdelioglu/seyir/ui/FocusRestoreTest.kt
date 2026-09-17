package io.github.knigdelioglu.seyir.ui

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusRestoreTest {
    @Test
    fun normalFocusTargetChange_doesNotChangeRestoreTrigger() {
        val previous = FocusRestoreKey(itemKeys = listOf("a", "b"), requestVersion = 0)
        val current = FocusRestoreKey(itemKeys = listOf("a", "b"), requestVersion = 0)

        assertFalse(shouldStartFocusRestore(previous, current))
    }

    @Test
    fun explicitRestoreRequest_changesRestoreTrigger() {
        val previous = FocusRestoreKey(itemKeys = listOf("a", "b"), requestVersion = 0)
        val current = FocusRestoreKey(itemKeys = listOf("a", "b"), requestVersion = 1)

        assertTrue(shouldStartFocusRestore(previous, current))
    }

    @Test
    fun datasetChange_changesRestoreTrigger() {
        val previous = FocusRestoreKey(itemKeys = listOf("a", "b"), requestVersion = 0)
        val current = FocusRestoreKey(itemKeys = listOf("a", "c"), requestVersion = 0)

        assertTrue(shouldStartFocusRestore(previous, current))
    }

    @Test
    fun missingTarget_usesFirstAvailableItemAsSafeFallback() {
        assertEquals(0, resolveFocusTargetIndex(listOf("a", "b"), "removed"))
        assertEquals(0, resolveFocusTargetIndex(listOf("a", "b"), null))
        assertEquals(null, resolveFocusTargetIndex(emptyList(), "removed"))
    }

    @Test
    fun removedItem_usesNextItem_thenPreviousItem_asFallback() {
        assertEquals("c", adjacentFallbackKey(listOf("a", "b", "c"), "b"))
        assertEquals("b", adjacentFallbackKey(listOf("a", "b", "c"), "c"))
        assertEquals(null, adjacentFallbackKey(listOf("a"), "a"))
    }

    @Test
    fun visibleTarget_requestsFocus_withoutStartingAnotherScroll() = runBlocking {
        var scrollCalls = 0
        var requestedKey: String? = null

        val restored = restoreItemFocus(
            itemKeys = listOf("a", "b"),
            focusTarget = "b",
            isItemVisible = { true },
            scrollToItem = {
                scrollCalls += 1
            },
            requestFocus = { requestedKey = it },
        )

        assertTrue(restored)
        assertEquals(0, scrollCalls)
        assertEquals("b", requestedKey)
    }
}
