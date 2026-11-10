package com.vibra.navigation.presentation.guidance

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.vibra.navigation.R
import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.presentation.components.ConnectionIndicator
import com.vibra.navigation.presentation.components.HapticCommand
import com.vibra.navigation.presentation.components.HapticSignature
import com.vibra.navigation.presentation.components.Pill
import com.vibra.navigation.presentation.components.ProgressBar
import com.vibra.navigation.presentation.components.SurfaceCard
import com.vibra.navigation.presentation.components.VibraBackground
import com.vibra.navigation.presentation.components.VibraButtons
import com.vibra.navigation.presentation.components.BrandWaves
import com.vibra.navigation.presentation.components.VibraLogoBadge
import com.vibra.navigation.presentation.components.formatDistance
import com.vibra.navigation.presentation.theme.VibraColors
import com.vibra.navigation.presentation.theme.VibraGradients
import com.vibra.navigation.presentation.theme.VibraTheme

@Composable
fun GuidanceRoute(viewModel: GuidanceViewModel = viewModel(factory = GuidanceViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleStartEffect(Unit) {
        viewModel.refresh()
        onStopOrDispose { }
    }
    GuidanceScreen(
        state = state,
        onRepeat = viewModel::repeatInstruction,
        onTogglePause = viewModel::togglePause,
        onStop = viewModel::stopNavigation,
    )
}

/**
 * Navegación activa, pensada para leerse de un vistazo:
 *  1. maniobra (icono grande en un círculo) + distancia en naranja + instrucción,
 *     o el panel de alerta si el teléfono envió ALERT;
 *  2. destino, barra de progreso y distancia restante;
 *  3. acciones: repetir, pausar y detener (en rojo, al final para no pulsarla sin querer).
 */
@Composable
fun GuidanceScreen(
    state: GuidanceUiState,
    onRepeat: () -> Unit,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
) {
    // La maniobra (ítem 0) es lo primero que debe verse al levantar la muñeca.
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    VibraBackground {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            ScalingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (!state.hasGuidance) {
                    item(key = "idle") { IdleGuidance(state.connection) }
                    return@ScalingLazyColumn
                }

                item(key = "maneuver") {
                    if (state.isAlert) {
                        AlertPanel(state.maneuver?.instruction.orEmpty())
                    } else {
                        ManeuverPanel(state.maneuver, state.isPaused)
                    }
                }
                item(key = "paused") {
                    AnimatedVisibility(
                        visible = state.isPaused,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Pill(
                            text = stringResource(R.string.guidance_paused_banner),
                            background = VibraColors.OrangeSoft,
                            color = VibraColors.OnOrange,
                        )
                    }
                }
                if (state.route.routeActive) {
                    item(key = "route") { RouteProgressCard(state.route, state.routeProgress) }
                }
                item(key = "repeat") {
                    Button(
                        onClick = onRepeat,
                        enabled = state.maneuver != null,
                        colors = VibraButtons.primary(),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.action_repeat), fontWeight = FontWeight.Bold) },
                    )
                }
                item(key = "pause") {
                    Button(
                        onClick = onTogglePause,
                        colors = VibraButtons.secondary(),
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                stringResource(if (state.isPaused) R.string.action_resume else R.string.action_pause),
                                fontWeight = FontWeight.Bold,
                            )
                        },
                    )
                }
                if (state.route.routeActive) {
                    item(key = "stop") {
                        Button(
                            onClick = onStop,
                            colors = VibraButtons.danger(),
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.action_stop), fontWeight = FontWeight.Bold) },
                        )
                    }
                }
                state.message?.let { message ->
                    item(key = "message") {
                        Text(
                            text = stringResource(message),
                            color = VibraColors.DangerText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/** Icono de maniobra en un círculo, distancia hasta la maniobra e instrucción. */
@Composable
private fun ManeuverPanel(maneuver: ManeuverUi?, isPaused: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            // TalkBack anuncia cada indicación nueva sin tocar la pantalla.
            .semantics { liveRegion = LiveRegionMode.Assertive },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                // Aro de marca azul → naranja alrededor de la maniobra.
                .background(if (isPaused) SolidColor(VibraColors.Outline) else VibraGradients.BrandSweep, CircleShape)
                .padding(4.dp)
                .background(VibraGradients.Card, CircleShape),
        ) {
            ManeuverIndicator(
                action = maneuver?.action ?: NavigationAction.STRAIGHT,
                eventId = maneuver?.id ?: 0L,
                color = if (maneuver == null || isPaused) VibraColors.Disabled else VibraColors.OnSurface,
                size = 64.dp,
            )
        }
        AnimatedContent(
            targetState = maneuver?.distanceMeters,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.7f)) togetherWith fadeOut() },
            label = "distance",
        ) { meters ->
            if (meters != null) {
                Text(
                    text = formatDistance(meters.toDouble()),
                    color = if (isPaused) VibraColors.OnSurfaceMuted else VibraColors.OrangeText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Spacer(Modifier.height(4.dp))
            }
        }
        AnimatedContent(
            targetState = maneuver,
            contentKey = { it?.id },
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith
                    (slideOutVertically { -it / 2 } + fadeOut()) using SizeTransform(clip = false)
            },
            label = "instruction",
        ) { current ->
            Text(
                text = current?.instruction?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.waiting_instruction),
                color = if (current == null) VibraColors.OnSurfaceMuted else VibraColors.OnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        // Comando háptico: no solo la flecha, también el ritmo que se siente.
        HapticCommand.of(maneuver?.action)?.let { command ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .background(VibraColors.Surface, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                HapticSignature(
                    waveform = command.preset.waveform(),
                    color = if (isPaused) VibraColors.Disabled else command.accent,
                    width = 54.dp,
                    height = 12.dp,
                )
                Text(
                    text = stringResource(command.hint),
                    color = VibraColors.OnSurfaceMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

/** Pantalla de ALERT: triángulo de aviso ámbar, título y texto centrados. */
@Composable
private fun AlertPanel(instruction: String) {
    val title = stringResource(R.string.alert_title)
    val body = instruction.ifBlank { stringResource(R.string.alert_body_default) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Assertive
                contentDescription = "$title. $body"
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(92.dp)
                .background(VibraColors.Warning.copy(alpha = 0.16f), CircleShape),
        ) {
            ManeuverIndicator(
                action = NavigationAction.ALERT,
                eventId = 0L,
                color = VibraColors.Warning,
                size = 60.dp,
            )
        }
        Text(
            text = title,
            color = VibraColors.Warning,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = body,
            color = VibraColors.OnSurface,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}

/** Destino en píldora, barra de progreso y distancia restante. */
@Composable
private fun RouteProgressCard(route: ActiveRoute, progress: Float?) {
    SurfaceCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            route.currentDestination?.let {
                Text(
                    text = it,
                    color = VibraColors.OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val progressDescription = progress?.let {
                stringResource(R.string.route_progress_cd, (it * 100).toInt())
            }
            ProgressBar(
                fraction = progress ?: 0f,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .semantics { progressDescription?.let { contentDescription = it } },
            )
            route.remainingDistanceMeters?.let {
                Text(
                    text = stringResource(R.string.remaining_label, formatDistance(it)),
                    color = VibraColors.OnSurfaceMuted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun IdleGuidance(connection: ConnectionStatus) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Logo con las ondas de marca latiendo despacio mientras espera la ruta.
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(104.dp)) {
            BrandWaves(
                modifier = Modifier.size(104.dp),
                waves = 3,
                startFraction = 0.34f,
                travelFraction = 0.15f,
                strokeWidth = 3.5.dp,
                periodMillis = 2_200,
                maxAlpha = 0.7f,
            )
            VibraLogoBadge(size = 62.dp, ring = true)
        }
        Text(
            text = stringResource(R.string.waiting_title),
            color = VibraColors.OnSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = stringResource(R.string.waiting_body),
            color = VibraColors.OnSurfaceMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        if (connection !is ConnectionStatus.Connected) ConnectionIndicator(connection)
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun GuidancePreview() {
    VibraTheme {
        AppScaffold {
            GuidanceScreen(
                state = GuidanceUiState(
                    connection = ConnectionStatus.Connected("Pixel", isNearby = true),
                    route = ActiveRoute("Plaza de Nariño", 820.0, routeActive = true),
                    routeProgress = 0.4f,
                    maneuver = ManeuverUi(1L, NavigationAction.TURN_RIGHT, "Gira a la derecha en 30 metros", 30),
                ),
                onRepeat = {},
                onTogglePause = {},
                onStop = {},
            )
        }
    }
}

@WearPreviewDevices
@Composable
private fun AlertPreview() {
    VibraTheme {
        AppScaffold {
            GuidanceScreen(
                state = GuidanceUiState(
                    connection = ConnectionStatus.Connected("Pixel", isNearby = true),
                    route = ActiveRoute("Plaza de Nariño", 820.0, routeActive = true),
                    maneuver = ManeuverUi(2L, NavigationAction.ALERT, "Atención: revisa tu ruta en el teléfono", 0),
                ),
                onRepeat = {},
                onTogglePause = {},
                onStop = {},
            )
        }
    }
}
