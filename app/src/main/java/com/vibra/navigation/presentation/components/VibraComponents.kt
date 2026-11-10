package com.vibra.navigation.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.vibra.navigation.presentation.theme.VibraGradients

/** Fondo de marca constante para todas las pantallas: marfil pastel liso. */
@Composable
fun VibraBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VibraColors.Background),
        content = content,
    )
}

/**
 * Ondas de vibración del logo: arcos azules a la izquierda y naranjas a la
 * derecha que se expanden y se desvanecen. [startFraction] es el radio inicial
 * relativo al lienzo (0.5 = borde). Con [animated] = false quedan fijas.
 */
@Composable
fun BrandWaves(
    modifier: Modifier = Modifier,
    waves: Int = 3,
    startFraction: Float = 0.30f,
    travelFraction: Float = 0.18f,
    strokeWidth: Dp = 4.dp,
    sweep: Float = 70f,
    periodMillis: Int = 1_400,
    maxAlpha: Float = 0.9f,
    animated: Boolean = true,
) {
    val progress = if (animated) {
        rememberInfiniteTransition(label = "brandWaves").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Restart),
            label = "brandWave",
        ).value
    } else {
        0f
    }
    Canvas(modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        for (i in 0 until waves) {
            val t = if (animated) (progress + i.toFloat() / waves) % 1f else i.toFloat() / waves
            val radius = size.minDimension * (startFraction + travelFraction * t)
            // Aparecen rápido y se apagan al alejarse.
            val alpha = maxAlpha * (if (t < 0.15f) t / 0.15f else 1f - (t - 0.15f) / 0.85f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arc = Size(radius * 2, radius * 2)
            drawArc(VibraColors.Blue.copy(alpha = alpha), 180f - sweep / 2, sweep, false, topLeft, arc, style = stroke)
            drawArc(VibraColors.Orange.copy(alpha = alpha), -sweep / 2, sweep, false, topLeft, arc, style = stroke)
        }
    }
}

/** Tarjeta marina con degradado y borde fino: agrupa información secundaria. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VibraGradients.Card)
            .border(1.dp, VibraColors.Outline.copy(alpha = 0.6f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        content = content,
    )
}

/**
 * Logo de Vibra sobre una insignia crema: el logo tiene trazos marino que se
 * perderían sobre el fondo de marca. Con [ring] lleva un aro azul → naranja.
 */
@Composable
fun VibraLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    ring: Boolean = false,
    fill: Color = Color.White,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .then(
                if (ring) {
                    Modifier
                        .background(VibraGradients.BrandSweep, CircleShape)
                        .padding(size * 0.04f)
                } else {
                    Modifier
                },
            )
            .background(fill, CircleShape),
    ) {
        Image(
            painter = painterResource(R.drawable.vibra_logo),
            contentDescription = null,
            modifier = Modifier.size(size * 0.74f),
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
            ConnectionStatus.Unknown -> VibraColors.OrangeText
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
    brush: Brush = VibraGradients.Brand,
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
                    .background(brush, RoundedCornerShape(50)),
            )
        }
    }
}

object VibraButtons {
    /** Acción principal: naranja con texto marino (≈ 7:1). */
    @Composable
    fun primary(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = VibraColors.Orange,
        contentColor = VibraColors.OnOrange,
        disabledContainerColor = VibraColors.Surface,
        disabledContentColor = VibraColors.Disabled,
    )

    /** Acción de marca: azul del logo con texto blanco. */
    @Composable
    fun brand(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = VibraColors.Blue,
        contentColor = Color.White,
        secondaryContentColor = Color.White.copy(alpha = 0.85f),
        iconColor = Color.White,
        disabledContainerColor = VibraColors.Surface,
        disabledContentColor = VibraColors.Disabled,
    )

    /** Acción secundaria: arena clara con texto marino. */
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
