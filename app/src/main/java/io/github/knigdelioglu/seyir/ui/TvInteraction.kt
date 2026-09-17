package io.github.knigdelioglu.seyir.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Compose's generic clickable handles ENTER on the TV box, but some remotes
 * report the OK button as DPAD_CENTER. Keep both activation paths consistent
 * for custom focusable controls.
 */
fun Modifier.tvDpadClick(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = if (!enabled) {
    this
} else {
    onPreviewKeyEvent { event ->
        val nativeEvent = event.nativeKeyEvent
        if (
            event.type == KeyEventType.KeyDown &&
            nativeEvent.repeatCount == 0 &&
            nativeEvent.keyCode in TV_ACTIVATION_KEYS
        ) {
            onClick()
            true
        } else {
            false
        }
    }
}

private val TV_ACTIVATION_KEYS = setOf(
    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
    AndroidKeyEvent.KEYCODE_ENTER,
    AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
    AndroidKeyEvent.KEYCODE_BUTTON_A,
)
