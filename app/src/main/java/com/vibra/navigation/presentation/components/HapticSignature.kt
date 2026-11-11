package com.vibra.navigation.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibra.navigation.R
import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.haptics.HapticPreset
import com.vibra.navigation.haptics.HapticWaveform
import com.vibra.navigation.presentation.theme.VibraColors

/** Los cuatro comandos hápticos distintivos de la navegación. */
enum class HapticCommand(
    val action: NavigationAction,
    val preset: HapticPreset,
    @param:StringRes val label: Int,
    @param:StringRes val hint: Int,
    val accent: Color,
) {
    LEFT(NavigationAction.TURN_LEFT, HapticPreset.LEFT, R.string.haptics_left, R.string.haptics_left_hint, VibraColors.Blue),
    RIGHT(NavigationAction.TURN_RIGHT, HapticPreset.RIGHT, R.string.haptics_right, R.string.haptics_right_hint, VibraColors.Orange),
    STRAIGHT(NavigationAction.STRAIGHT, HapticPreset.STRAIGHT, R.string.haptics_straight, R.string.haptics_straight_hint, VibraColors.Emerald),
    U_TURN(NavigationAction.U_TURN, HapticPreset.U_TURN, R.string.haptics_u_turn, R.string.haptics_u_turn_hint, VibraColors.Warning),
    ;

    companion object {
        fun of(action: NavigationAction?): HapticCommand? = entries.firstOrNull { it.action == action }
    }
}

/**
 * Firma visual de un patrón háptico: cada pulso es una barra cuyo ancho es su
 * duración y su altura la fuerza; los huecos son las pausas. Así se "ve" el ritmo
 * que se siente en la muñeca.
 */
@Composable
fun HapticSignature(
    waveform: HapticWaveform,
    color: Color,
    modifier: Modifier = Modifier,
    width: Dp = 84.dp,
    height: Dp = 16.dp,
    /** Para lectores de pantalla (p. ej. "Doble pulso"); null si el texto ya está al lado. */
    description: String? = null,
) {
    val semanticsModifier = if (description != null) {
        Modifier.semantics { contentDescription = description }
    } else {
        Modifier.clearAndSetSemantics { }
    }
    Canvas(modifier.then(semanticsModifier).width(width).height(height)) {
        val total = waveform.totalDurationMs.coerceAtLeast(1L).toFloat()
        val radius = CornerRadius(2.dp.toPx())
        var t = 0L
        for (i in waveform.timings.indices) {
            val d = waveform.timings[i]
            val a = waveform.amplitudes[i]
            if (a > 0) {
                val h = size.height * (0.35f + 0.65f * a / 255f)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * t / total, (size.height - h) / 2f),
                    size = Size((size.width * d / total).coerceAtLeast(2.dp.toPx()), h),
                    cornerRadius = radius,
                )
            }
            t += d
        }
    }
}
