package io.github.knigdelioglu.seyir.ui

import android.view.KeyEvent as AndroidKeyEvent
import android.view.ViewConfiguration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
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

private val GOOGLE_NEON_COLORS = intArrayOf(
    android.graphics.Color.rgb(0x42, 0x85, 0xF4), // Google Blue
    android.graphics.Color.rgb(0xEA, 0x43, 0x35), // Google Red
    android.graphics.Color.rgb(0xFB, 0xBC, 0x05), // Google Yellow
    android.graphics.Color.rgb(0x34, 0xA8, 0x53), // Google Green
    android.graphics.Color.rgb(0x42, 0x85, 0xF4), // Google Blue (closes the loop)
)

/**
 * Draws an animated Google TV style neon pulsing border around the card when focused.
 * The 4 Google neon colors (Blue, Red, Yellow, Green) rotate and pulse with a breathing glow.
 */
@Composable
fun Modifier.tvFocusedChasingBorder(
    focused: Boolean,
    cornerRadius: Dp = SeyirRadius.Card,
    borderWidth: Dp = 2.8.dp,
): Modifier {
    if (!focused) return this

    val infiniteTransition = rememberInfiniteTransition(label = "google-neon-border")

    // Continuous flow/rotation of the 4 neon colors around the perimeter
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )

    // Neon breathing / pulsing (yanıp sönme) like Google TV startup
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val matrix = remember { android.graphics.Matrix() }

    return drawWithContent {
        drawContent()

        val strokePx = borderWidth.toPx()
        val halfStroke = strokePx / 2f
        val cornerPx = cornerRadius.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f

        val shader = android.graphics.SweepGradient(cx, cy, GOOGLE_NEON_COLORS, null)
        matrix.setRotate(angle, cx, cy)
        shader.setLocalMatrix(matrix)
        val brush = ShaderBrush(shader)

        val strokeCornerRadius = CornerRadius(
            maxOf(0f, cornerPx - halfStroke),
            maxOf(0f, cornerPx - halfStroke),
        )

        // 1. Soft outer neon glow bloom
        drawRoundRect(
            brush = brush,
            topLeft = Offset(halfStroke, halfStroke),
            size = Size(size.width - strokePx, size.height - strokePx),
            cornerRadius = strokeCornerRadius,
            style = Stroke(width = strokePx * 2.2f),
            alpha = pulse * 0.35f,
        )

        // 2. Main vivid neon core stroke
        drawRoundRect(
            brush = brush,
            topLeft = Offset(halfStroke, halfStroke),
            size = Size(size.width - strokePx, size.height - strokePx),
            cornerRadius = strokeCornerRadius,
            style = Stroke(width = strokePx),
            alpha = pulse * 0.95f,
        )
    }
}

