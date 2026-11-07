package com.vibra.navigation.presentation.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibra.navigation.R
import com.vibra.navigation.data.model.NavigationAction

/**
 * Flecha dibujada con Canvas (sin dependencia de iconos) que rota por el camino
 * más corto entre maniobras y hace un "pulso" de escala con cada evento nuevo.
 */
@Composable
fun ManeuverIndicator(
    action: NavigationAction,
    eventId: Long,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    // Ángulo acumulado: evita que pasar de -90° a 180° dé una vuelta de 270°.
    var targetAngle by remember { mutableFloatStateOf(action.angle()) }
    LaunchedEffect(action) {
        val diff = ((action.angle() - targetAngle) % 360f + 540f) % 360f - 180f
        targetAngle += diff
    }
    val angle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "maneuverAngle",
    )

    val pulse = remember { Animatable(1f) }
    LaunchedEffect(eventId) {
        pulse.snapTo(1.2f)
        pulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    val spin = if (action == NavigationAction.REROUTING) {
        val transition = rememberInfiniteTransition(label = "rerouting")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1_200, easing = LinearEasing), RepeatMode.Restart),
            label = "reroutingSpin",
        ).value
    } else {
        0f
    }

    val description = stringResource(R.string.cd_maneuver, stringResource(action.labelRes()))

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            }
            .semantics { contentDescription = description },
    ) {
        when (action) {
            NavigationAction.ARRIVED -> drawArrived(color)
            NavigationAction.REROUTING -> rotate(spin) { drawRerouting(color) }
            else -> rotate(angle) { drawArrow(color) }
        }
    }
}

private fun DrawScope.drawArrow(color: Color) {
    val w = size.width
    val h = size.height
    val stroke = w * 0.16f
    // Cuerpo
    drawLine(color, Offset(w / 2, h * 0.92f), Offset(w / 2, h * 0.30f), strokeWidth = stroke, cap = StrokeCap.Round)
    // Punta
    val head = Path().apply {
        moveTo(w / 2, h * 0.04f)
        lineTo(w * 0.90f, h * 0.46f)
        lineTo(w * 0.10f, h * 0.46f)
        close()
    }
    drawPath(head, color)
}

private fun DrawScope.drawArrived(color: Color) {
    val w = size.width
    drawCircle(color, radius = w * 0.44f, style = Stroke(width = w * 0.08f))
    val check = Path().apply {
        moveTo(w * 0.28f, w * 0.52f)
        lineTo(w * 0.44f, w * 0.68f)
        lineTo(w * 0.74f, w * 0.36f)
    }
    drawPath(check, color, style = Stroke(width = w * 0.10f, cap = StrokeCap.Round))
}

private fun DrawScope.drawRerouting(color: Color) {
    val w = size.width
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

private fun NavigationAction.angle(): Float = when (this) {
    NavigationAction.TURN_LEFT -> -90f
    NavigationAction.TURN_RIGHT -> 90f
    NavigationAction.SLIGHT_LEFT -> -45f
    NavigationAction.SLIGHT_RIGHT -> 45f
    NavigationAction.U_TURN -> 180f
    else -> 0f
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
