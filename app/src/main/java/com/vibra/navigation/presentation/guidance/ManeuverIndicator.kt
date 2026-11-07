package com.vibra.navigation.presentation.guidance

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibra.navigation.R
import com.vibra.navigation.data.model.NavigationAction

/**
 * Flecha curva gigante, como la del mockup: sube y gira hacia el lado de la
 * maniobra. Cambia con una transición de escala/fundido y hace un "pulso" con
 * cada evento nuevo, para reforzar visualmente la vibración.
 */
@Composable
fun ManeuverIndicator(
    action: NavigationAction,
    eventId: Long,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(eventId) {
        pulse.snapTo(1.18f)
        pulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    val description = stringResource(R.string.cd_maneuver, stringResource(action.labelRes()))

    AnimatedContent(
        targetState = action,
        transitionSpec = {
            (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 1.3f) + fadeOut())
        },
        label = "maneuver",
        modifier = modifier
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            }
            .semantics { contentDescription = description },
    ) { current ->
        if (current == NavigationAction.REROUTING) {
            val spin = rememberInfiniteTransition(label = "rerouting").animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(1_200, easing = LinearEasing), RepeatMode.Restart),
                label = "reroutingSpin",
            )
            Canvas(Modifier.size(size)) { rotate(spin.value) { drawRerouting(color) } }
        } else {
            Canvas(Modifier.size(size)) { drawManeuver(current, color) }
        }
    }
}

/** Dibuja en un lienzo lógico de 100×100 escalado al tamaño real. */
private fun DrawScope.drawManeuver(action: NavigationAction, color: Color) {
    val k = size.minDimension / 100f
    fun p(x: Float, y: Float) = Offset(x * k, y * k)
    val shaft = Stroke(width = 14f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun head(tip: Offset, left: Offset, right: Offset) {
        val path = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(left.x, left.y)
            lineTo(right.x, right.y)
            close()
        }
        drawPath(path, color)
    }

    fun turnRight() {
        drawPath(
            Path().apply {
                moveTo(34 * k, 92 * k)
                lineTo(34 * k, 58 * k)
                quadraticTo(34 * k, 30 * k, 64 * k, 30 * k)
            },
            color,
            style = shaft,
        )
        head(p(95f, 30f), p(64f, 7f), p(64f, 53f))
    }

    fun slightRight() {
        drawPath(
            Path().apply {
                moveTo(38 * k, 92 * k)
                lineTo(38 * k, 66 * k)
                quadraticTo(38 * k, 48 * k, 64 * k, 34 * k)
            },
            color,
            style = shaft,
        )
        head(p(86f, 12f), p(52f, 18f), p(80f, 46f))
    }

    when (action) {
        NavigationAction.TURN_RIGHT -> turnRight()
        NavigationAction.SLIGHT_RIGHT -> slightRight()
        // Izquierda = espejo horizontal de la derecha.
        NavigationAction.TURN_LEFT -> scale(-1f, 1f) { turnRight() }
        NavigationAction.SLIGHT_LEFT -> scale(-1f, 1f) { slightRight() }
        NavigationAction.U_TURN -> {
            drawPath(
                Path().apply {
                    moveTo(66 * k, 92 * k)
                    lineTo(66 * k, 42 * k)
                    arcTo(
                        rect = Rect(Offset(34 * k, 24 * k), Size(32 * k, 36 * k)),
                        startAngleDegrees = 0f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false,
                    )
                    lineTo(34 * k, 60 * k)
                },
                color,
                style = shaft,
            )
            head(p(34f, 92f), p(12f, 60f), p(56f, 60f))
        }
        NavigationAction.ARRIVED -> {
            drawCircle(color, radius = 44f * k, style = Stroke(width = 8f * k))
            drawPath(
                Path().apply {
                    moveTo(28 * k, 52 * k)
                    lineTo(44 * k, 68 * k)
                    lineTo(74 * k, 36 * k)
                },
                color,
                style = Stroke(width = 10f * k, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        else -> { // STRAIGHT / UNKNOWN
            drawLine(color, p(50f, 92f), p(50f, 32f), strokeWidth = 14f * k, cap = StrokeCap.Round)
            head(p(50f, 6f), p(74f, 38f), p(26f, 38f))
        }
    }
}

private fun DrawScope.drawRerouting(color: Color) {
    val w = size.minDimension
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(w * 0.12f, w * 0.12f),
        size = Size(w * 0.76f, w * 0.76f),
        style = Stroke(width = w * 0.10f, cap = StrokeCap.Round),
    )
}

@StringRes
private fun NavigationAction.labelRes(): Int = when (this) {
    NavigationAction.TURN_LEFT -> R.string.maneuver_turn_left
    NavigationAction.TURN_RIGHT -> R.string.maneuver_turn_right
    NavigationAction.SLIGHT_LEFT -> R.string.maneuver_slight_left
    NavigationAction.SLIGHT_RIGHT -> R.string.maneuver_slight_right
    NavigationAction.STRAIGHT -> R.string.maneuver_straight
    NavigationAction.U_TURN -> R.string.maneuver_u_turn
    NavigationAction.ARRIVED -> R.string.maneuver_arrived
    NavigationAction.REROUTING -> R.string.maneuver_rerouting
    NavigationAction.UNKNOWN -> R.string.maneuver_unknown
}
