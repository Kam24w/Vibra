package com.vibra.navigation.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import com.vibra.navigation.R
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.presentation.theme.VibraColors

/** Fondo negro puro (OLED) para todas las pantallas. */
@Composable
fun VibraBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VibraColors.Background),
        content = content,
    )
}

/** Tarjeta pizarra redondeada: agrupa información secundaria sin encender el panel. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VibraColors.Surface, RoundedCornerShape(cornerRadius))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        content = content,
    )
}

/**
 * Logo de Vibra sobre una insignia clara: el logo tiene trazos marino oscuro que
 * desaparecerían directamente sobre negro.
 */
@Composable
fun VibraLogoBadge(modifier: Modifier = Modifier, size: Dp = 30.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(VibraColors.OnSurface, CircleShape),
    ) {
        Image(
            painter = painterResource(R.drawable.vibra_logo),
            contentDescription = null,
            modifier = Modifier.size(size * 0.78f),
        )
    }
}

/** Logo + palabra "Vibra". */
@Composable
fun VibraHeader(modifier: Modifier = Modifier, logoSize: Dp = 28.dp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        VibraLogoBadge(size = logoSize)
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.app_name),
            color = VibraColors.OnSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** Píldora de texto (p. ej. el destino). */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = VibraColors.SurfaceHigh,
    color: Color = VibraColors.OnSurface,
) {
    Text(
        text = text,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

/** Punto de estado de color. */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).background(color, CircleShape))
}

/**
 * Indicador sutil del enlace con WanderAI: punto verde (conectado), naranja
 * latiendo (buscando) o rojo (sin teléfono) y una etiqueta corta.
 */
@Composable
fun ConnectionIndicator(connection: ConnectionStatus, modifier: Modifier = Modifier) {
    val label = when (connection) {
        is ConnectionStatus.Connected -> stringResource(R.string.status_connected)
        ConnectionStatus.Unknown -> stringResource(R.string.status_searching)
        ConnectionStatus.Disconnected -> stringResource(R.string.status_disconnected)
        is ConnectionStatus.Unavailable -> stringResource(R.string.status_unavailable)
    }
    val dotColor by animateColorAsState(
        when (connection) {
            is ConnectionStatus.Connected -> VibraColors.Emerald
            ConnectionStatus.Unknown -> VibraColors.Orange
            else -> VibraColors.DangerText
        },
        label = "connectionDot",
    )
    val pulse by rememberInfiniteTransition(label = "searching").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "searchingPulse",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = label },
    ) {
        StatusDot(
            color = dotColor,
            modifier = Modifier.alpha(if (connection is ConnectionStatus.Unknown) pulse else 1f),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = VibraColors.OnSurfaceMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** Barra de progreso redondeada y animada; [fraction] entre 0 y 1. */
@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = VibraColors.Orange,
    track: Color = VibraColors.SurfaceHigh,
    height: Dp = 6.dp,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(track, RoundedCornerShape(50)),
    ) {
        if (animated > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .background(color, RoundedCornerShape(50)),
            )
        }
    }
}

object VibraButtons {
    /** Acción principal: naranja con texto negro (≈ 7:1). */
    @Composable
    fun primary(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = VibraColors.Orange,
        contentColor = VibraColors.OnOrange,
        disabledContainerColor = VibraColors.Surface,
        disabledContentColor = VibraColors.Disabled,
    )

    /** Acción secundaria: pizarra con texto claro. */
    @Composable
    fun secondary(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = VibraColors.SurfaceHigh,
        contentColor = VibraColors.OnSurface,
        secondaryContentColor = VibraColors.OnSurfaceMuted,
        disabledContainerColor = VibraColors.Surface,
        disabledContentColor = VibraColors.Disabled,
    )

    /** Acción destructiva (detener ruta): rojo de alerta con texto blanco (AA). */
    @Composable
    fun danger(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = VibraColors.Danger,
        contentColor = VibraColors.OnDanger,
        disabledContainerColor = VibraColors.Surface,
        disabledContentColor = VibraColors.Disabled,
    )
}
