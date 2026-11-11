package com.vibra.navigation.presentation.link

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.vibra.navigation.R
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.data.repository.NavigationRepository
import com.vibra.navigation.presentation.components.VibraBackground
import com.vibra.navigation.presentation.components.containerViewModelFactory
import com.vibra.navigation.presentation.theme.VibraColors
import com.vibra.navigation.presentation.theme.VibraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LinkSearchViewModel(private val repository: NavigationRepository) : ViewModel() {
    val connection: StateFlow<ConnectionStatus> = repository.connectionStatus

    init {
        // Mientras se busca, reintenta cada pocos segundos hasta encontrar el teléfono.
        viewModelScope.launch {
            while (isActive && repository.connectionStatus.value !is ConnectionStatus.Connected) {
                repository.refresh()
                delay(RETRY_MS)
            }
        }
    }

    companion object {
        private const val RETRY_MS = 3_000L
        val Factory = containerViewModelFactory { c -> LinkSearchViewModel(c.navigationRepository) }
    }
}

@Composable
fun LinkSearchRoute(
    onLinked: () -> Unit,
    viewModel: LinkSearchViewModel = viewModel(factory = LinkSearchViewModel.Factory),
) {
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val linked by rememberUpdatedState(onLinked)
    // Al encontrar el teléfono se muestra la confirmación un momento y se sigue al inicio.
    LaunchedEffect(connection is ConnectionStatus.Connected) {
        if (connection is ConnectionStatus.Connected) {
            delay(LINKED_HOLD_MS)
            linked()
        }
    }
    LinkSearchScreen(connection = connection, onSkip = onLinked)
}

/**
 * Pantalla 3: búsqueda del teléfono anfitrión. Un radar de anillos concéntricos
 * que se expanden y un haz que gira alrededor del icono del celular. Al conectar,
 * el radar se detiene, el icono pasa a verde y se continúa solo al inicio.
 */
@Composable
fun LinkSearchScreen(connection: ConnectionStatus, onSkip: () -> Unit) {
    val found = connection is ConnectionStatus.Connected
    val title = when (connection) {
        is ConnectionStatus.Connected -> stringResource(R.string.link_found)
        is ConnectionStatus.Unavailable -> stringResource(R.string.status_unavailable)
        else -> stringResource(R.string.link_searching)
    }
    val body = when (connection) {
        is ConnectionStatus.Connected -> connection.nodeName
        else -> stringResource(R.string.link_hint)
    }
    VibraBackground {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp, start = 18.dp, end = 18.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(108.dp)) {
                Radar(active = !found, modifier = Modifier.size(108.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .background(if (found) VibraColors.Emerald else VibraColors.Blue, CircleShape),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_phone),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            AnimatedContent(
                targetState = title,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "linkTitle",
            ) { text ->
                Text(
                    text = text,
                    color = VibraColors.OnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = text
                        },
                )
            }
            Text(
                text = body,
                color = if (found) VibraColors.Emerald else VibraColors.OnSurfaceMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        if (!found) {
            EdgeButton(
                onClick = onSkip,
                buttonSize = EdgeButtonSize.ExtraSmall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibraColors.SurfaceHigh,
                    contentColor = VibraColors.OnSurface,
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Text(stringResource(R.string.link_skip), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Radar: tres anillos que nacen en el centro y se desvanecen, más un haz que gira. */
@Composable
private fun Radar(active: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "radar")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_100, easing = LinearEasing), RepeatMode.Restart),
        label = "radarPulse",
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2_600, easing = LinearEasing), RepeatMode.Restart),
        label = "radarSweep",
    )
    Canvas(modifier) {
        val maxR = size.minDimension / 2f
        val ring = Stroke(width = 1.5.dp.toPx())
        // Guías fijas del radar.
        for (f in listOf(0.45f, 0.72f, 0.98f)) {
            drawCircle(VibraColors.Outline.copy(alpha = 0.55f), radius = maxR * f, style = ring)
        }
        if (!active) {
            drawCircle(VibraColors.Emerald.copy(alpha = 0.6f), radius = maxR * 0.72f, style = Stroke(2.5.dp.toPx()))
            return@Canvas
        }
        // Haz giratorio con estela azul.
        rotate(sweep) {
            drawCircle(
                brush = Brush.sweepGradient(
                    0f to Color.Transparent,
                    0.80f to Color.Transparent,
                    1f to VibraColors.Blue.copy(alpha = 0.35f),
                    center = center,
                ),
                radius = maxR,
            )
            drawLine(
                VibraColors.Blue,
                start = center,
                end = Offset(center.x + maxR, center.y),
                strokeWidth = 2.dp.toPx(),
            )
        }
        // Ondas concéntricas que se expanden.
        for (i in 0 until 3) {
            val t = (pulse + i / 3f) % 1f
            drawCircle(
                color = VibraColors.Blue.copy(alpha = (1f - t) * 0.7f),
                radius = maxR * (0.25f + 0.75f * t),
                style = Stroke(width = 2.5.dp.toPx()),
            )
        }
    }
}

private const val LINKED_HOLD_MS = 1_100L

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun LinkSearchPreview() {
    VibraTheme {
        AppScaffold { LinkSearchScreen(ConnectionStatus.Unknown, onSkip = {}) }
    }
}
