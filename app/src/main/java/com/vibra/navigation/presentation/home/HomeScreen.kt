package com.vibra.navigation.presentation.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.vibra.navigation.R
import com.vibra.navigation.data.device.BatteryState
import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.presentation.components.NavyCard
import com.vibra.navigation.presentation.components.StatusDot
import com.vibra.navigation.presentation.components.VibraBackground
import com.vibra.navigation.presentation.components.VibraButtons
import com.vibra.navigation.presentation.components.VibraHeader
import com.vibra.navigation.presentation.components.formatElapsed
import com.vibra.navigation.presentation.components.rememberNow
import com.vibra.navigation.presentation.theme.VibraColors
import com.vibra.navigation.presentation.theme.VibraTheme

@Composable
fun HomeRoute(
    onOpenGuidance: () -> Unit,
    onOpenHaptics: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleStartEffect(Unit) {
        viewModel.refresh()
        onStopOrDispose { }
    }

    // Abre la navegación automáticamente cuando WanderAI inicia una ruta con el
    // dashboard visible. Solo en la transición inactiva -> activa, para no impedir
    // que el usuario vuelva al inicio deslizando.
    var lastRouteActive by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(state.route.routeActive) {
        if (lastRouteActive == false && state.route.routeActive) onOpenGuidance()
        lastRouteActive = state.route.routeActive
    }

    HomeScreen(state = state, onOpenGuidance = onOpenGuidance, onOpenHaptics = onOpenHaptics)
}

/** Dashboard del reloj: enlace con WanderAI, sincronización y batería háptica. */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenGuidance: () -> Unit,
    onOpenHaptics: () -> Unit,
) {
    val listState = rememberScalingLazyListState()
    val now = rememberNow()
    VibraBackground {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            ScalingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxWidth(),
            ) {
                item(key = "header") { VibraHeader() }
                item(key = "link") { LinkCard(state.connection) }
                item(key = "sync") { SyncRow(state.lastSyncAt, now) }
                item(key = "battery") { BatteryRow(state.battery) }
                item(key = "guidance") {
                    Button(
                        onClick = onOpenGuidance,
                        colors = VibraButtons.primary(),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.home_open_guidance), fontWeight = FontWeight.Bold) },
                        secondaryLabel = {
                            Text(
                                text = state.route.currentDestination
                                    ?.takeIf { state.route.routeActive }
                                    ?.let { stringResource(R.string.destination_format, it) }
                                    ?: stringResource(R.string.home_no_route),
                                maxLines = 1,
                            )
                        },
                    )
                }
                item(key = "haptics") {
                    Button(
                        onClick = onOpenHaptics,
                        colors = VibraButtons.navy(),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.home_open_haptics), fontWeight = FontWeight.Bold) },
                    )
                }
            }
        }
    }
}

/** Tarjeta "Emparejando…" del mockup: reloj ··· teléfono con el estado del enlace. */
@Composable
private fun LinkCard(connection: ConnectionStatus) {
    val searching = connection is ConnectionStatus.Unknown
    val connected = connection is ConnectionStatus.Connected
    val label = when (connection) {
        is ConnectionStatus.Connected -> stringResource(
            if (connection.isNearby) R.string.link_bluetooth else R.string.link_cloud,
        )
        ConnectionStatus.Unknown -> stringResource(R.string.status_searching)
        ConnectionStatus.Disconnected -> stringResource(R.string.status_disconnected)
        is ConnectionStatus.Unavailable -> stringResource(R.string.status_unavailable)
    }
    val linkColor by animateColorAsState(
        when {
            connected -> VibraColors.Orange
            searching -> VibraColors.OrangeSoft
            else -> VibraColors.CreamMuted
        },
        label = "linkColor",
    )

    NavyCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp)
                .semantics(mergeDescendants = true) { contentDescription = label },
        ) {
            Text(
                text = stringResource(R.string.link_title),
                color = VibraColors.CreamMuted,
                fontSize = 11.sp,
            )
            LinkIllustration(connected = connected, animate = !connected && searching, color = linkColor)
            Text(
                text = when (connection) {
                    is ConnectionStatus.Connected -> connection.nodeName
                    else -> label
                },
                color = VibraColors.Cream,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            if (connected) {
                Text(text = label, color = linkColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Reloj y teléfono unidos por puntos: animados mientras busca, fijos si hay enlace. */
@Composable
private fun LinkIllustration(connected: Boolean, animate: Boolean, color: Color) {
    val phase by rememberInfiniteTransition(label = "dots").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_200), RepeatMode.Restart),
        label = "dotsPhase",
    )
    Canvas(
        Modifier
            .padding(vertical = 6.dp)
            .size(width = 120.dp, height = 34.dp),
    ) {
        drawWatch(Offset(size.width * 0.12f, size.height / 2), VibraColors.Cream)
        drawPhone(Offset(size.width * 0.88f, size.height / 2), VibraColors.Cream)
        val dots = 5
        val start = size.width * 0.28f
        val end = size.width * 0.72f
        for (i in 0 until dots) {
            val x = start + (end - start) * i / (dots - 1)
            val alpha = when {
                connected -> 1f
                animate -> (1f - kotlin.math.abs(phase * (dots + 1) - 1 - i) / 2f).coerceIn(0.25f, 1f)
                else -> 0.35f
            }
            drawCircle(color.copy(alpha = alpha), radius = 3.dp.toPx(), center = Offset(x, size.height / 2))
        }
    }
}

private fun DrawScope.drawWatch(center: Offset, color: Color) {
    val w = 18.dp.toPx()
    val h = 22.dp.toPx()
    drawRoundRect(
        color,
        topLeft = Offset(center.x - w * 0.35f, center.y - h * 0.75f),
        size = Size(w * 0.7f, h * 1.5f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(2.dp.toPx()),
    )
    drawRoundRect(
        color,
        topLeft = Offset(center.x - w / 2, center.y - h / 2),
        size = Size(w, h),
        cornerRadius = CornerRadius(6.dp.toPx()),
    )
}

private fun DrawScope.drawPhone(center: Offset, color: Color) {
    val w = 16.dp.toPx()
    val h = 28.dp.toPx()
    drawRoundRect(
        color,
        topLeft = Offset(center.x - w / 2, center.y - h / 2),
        size = Size(w, h),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = Stroke(2.5.dp.toPx()),
    )
}

/** Indicador de sincronización en tiempo real: el punto late tras cada intercambio. */
@Composable
private fun SyncRow(lastSyncAt: Long?, now: Long) {
    val recent = lastSyncAt != null && now - lastSyncAt < 10_000
    val pulse by rememberInfiniteTransition(label = "sync").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "syncPulse",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        StatusDot(
            color = if (lastSyncAt == null) VibraColors.NavyMuted else VibraColors.Success,
            modifier = Modifier.alpha(if (recent) pulse else 1f),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.sync_label, formatElapsed(lastSyncAt, now)),
            color = VibraColors.Navy,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Batería del reloj (dispositivo háptico) con barra de nivel. */
@Composable
private fun BatteryRow(battery: BatteryState?) {
    val fraction by animateFloatAsState((battery?.percent ?: 0) / 100f, label = "battery")
    val barColor = when {
        battery == null -> VibraColors.NavyMuted
        battery.percent <= 15 -> VibraColors.Error
        else -> VibraColors.Navy
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.battery_label), color = VibraColors.Navy, fontSize = 12.sp)
            Text(
                text = battery?.let {
                    stringResource(if (it.isCharging) R.string.battery_charging else R.string.battery_percent, it.percent)
                } ?: "—",
                color = VibraColors.Navy,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            Modifier
                .padding(top = 3.dp)
                .fillMaxWidth()
                .height(8.dp)
                .background(VibraColors.Beige, RoundedCornerShape(50)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(barColor, RoundedCornerShape(50)),
            )
        }
    }
}

@WearPreviewDevices
@Composable
private fun HomePreview() {
    VibraTheme {
        AppScaffold {
            HomeScreen(
                state = HomeUiState(
                    connection = ConnectionStatus.Connected("Pixel 9", isNearby = true),
                    battery = BatteryState(85, isCharging = false),
                    lastSyncAt = System.currentTimeMillis(),
                    route = ActiveRoute("Pasto", 820.0, routeActive = true),
                ),
                onOpenGuidance = {},
                onOpenHaptics = {},
            )
        }
    }
}
