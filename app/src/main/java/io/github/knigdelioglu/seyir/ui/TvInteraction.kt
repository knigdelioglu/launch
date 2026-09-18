package io.github.knigdelioglu.seyir.ui

import android.view.KeyEvent as AndroidKeyEvent
import android.view.ViewConfiguration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.knigdelioglu.seyir.ui.theme.SeyirMotion
import io.github.knigdelioglu.seyir.ui.theme.SeyirRadius
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Compose's generic clickable handles ENTER on the TV box, but some remotes
 * report the OK button as DPAD_CENTER. Keep both activation paths consistent
 * for custom focusable controls, and support long click from TV remote OK / MENU keys.
 */
fun Modifier.tvDpadClick(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = if (!enabled) {
    this
} else composed {
    val scope = rememberCoroutineScope()
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    var isLongPressTriggered by remember { mutableStateOf(false) }

    onPreviewKeyEvent { event ->
        val nativeEvent = event.nativeKeyEvent
        val isActivationKey = nativeEvent.keyCode in TV_ACTIVATION_KEYS
        val isMenuKey = nativeEvent.keyCode == AndroidKeyEvent.KEYCODE_MENU

        if (isMenuKey && onLongClick != null) {
            if (event.type == KeyEventType.KeyDown && nativeEvent.repeatCount == 0) {
                onLongClick()
                true
            } else {
                event.type == KeyEventType.KeyDown
            }
        } else if (isActivationKey) {
            when (event.type) {
                KeyEventType.KeyDown -> {
                    if (nativeEvent.repeatCount == 0) {
                        isLongPressTriggered = false
                        if (onLongClick != null) {
                            longPressJob?.cancel()
                            longPressJob = scope.launch {
                                delay(ViewConfiguration.getLongPressTimeout().toLong())
                                isLongPressTriggered = true
                                onLongClick()
                            }
                        } else {
                            onClick()
                        }
                        true
                    } else {
                        true
                    }
                }
                KeyEventType.KeyUp -> {
                    if (onLongClick != null) {
                        longPressJob?.cancel()
                        longPressJob = null
                        if (!isLongPressTriggered) {
                            onClick()
                        }
                        isLongPressTriggered = false
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        } else {
            false
        }
    }
}

@Composable
fun Modifier.tvFocusScale(
    focused: Boolean,
    label: String,
    scaleOrigin: TransformOrigin = TransformOrigin.Center,
): Modifier {
    val scale = animateFloatAsState(
        targetValue = if (focused) SeyirMotion.FocusScale else 1f,
        animationSpec = tween(
            durationMillis = SeyirMotion.FocusDurationMs,
            easing = SeyirMotion.FocusEasing,
        ),
        label = label,
    ).value
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        transformOrigin = scaleOrigin
    }
}

private val TV_ACTIVATION_KEYS = setOf(
    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
    AndroidKeyEvent.KEYCODE_ENTER,
    AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
    AndroidKeyEvent.KEYCODE_BUTTON_A,
)

private val GOOGLE_NEON_COLORS = listOf(
    Color(0xFF4285F4),
    Color(0xFFEA4335),
    Color(0xFFFBBC05),
    Color(0xFF34A853),
    Color(0xFF4285F4),
)

/**
 * Draws the Google-TV-style neon focus border without a perpetual animation.
 *
 * The previous implementation ran two infinite transitions and rebuilt an
 * Android SweepGradient + ShaderBrush on every drawn frame while any card
 * stayed focused. TV launchers keep an item focused almost all the time, so
 * that turned an otherwise idle screen into a permanent render loop.
 *
 * drawWithCache keeps the gradient and geometry cached until size/density or
 * the modifier inputs change. Focus still gets a vivid neon border, but an
 * idle launcher no longer burns CPU/GPU just to keep the highlight alive.
 */
@Composable
fun Modifier.tvFocusedChasingBorder(
    focused: Boolean,
    cornerRadius: Dp = SeyirRadius.Card,
    borderWidth: Dp = 2.8.dp,
): Modifier {
    if (!focused) return this

    return drawWithCache {
        val strokePx = borderWidth.toPx()
        val halfStroke = strokePx / 2f
        val cornerPx = cornerRadius.toPx()
        val strokeSize = Size(
            width = size.width - strokePx,
            height = size.height - strokePx,
        )
        val strokeCornerRadius = CornerRadius(
            x = maxOf(0f, cornerPx - halfStroke),
            y = maxOf(0f, cornerPx - halfStroke),
        )
        val brush = Brush.sweepGradient(
            colors = GOOGLE_NEON_COLORS,
            center = Offset(size.width / 2f, size.height / 2f),
        )

        onDrawWithContent {
            drawContent()
            if (strokeSize.width <= 0f || strokeSize.height <= 0f) {
                return@onDrawWithContent
            }

            drawRoundRect(
                brush = brush,
                topLeft = Offset(halfStroke, halfStroke),
                size = strokeSize,
                cornerRadius = strokeCornerRadius,
                style = Stroke(width = strokePx * 2f),
                alpha = 0.20f,
            )
            drawRoundRect(
                brush = brush,
                topLeft = Offset(halfStroke, halfStroke),
                size = strokeSize,
                cornerRadius = strokeCornerRadius,
                style = Stroke(width = strokePx),
                alpha = 0.95f,
            )
        }
    }
}
