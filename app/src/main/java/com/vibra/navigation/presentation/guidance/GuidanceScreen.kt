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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
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
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.vibra.navigation.R
import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.presentation.components.NavyCard
import com.vibra.navigation.presentation.components.NavyPill
import com.vibra.navigation.presentation.components.StatusDot
import com.vibra.navigation.presentation.components.VibraBackground
import com.vibra.navigation.presentation.components.VibraButtons
import com.vibra.navigation.presentation.components.VibraHeader
import com.vibra.navigation.presentation.components.formatDistance
import com.vibra.navigation.presentation.theme.VibraColors
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
 * Pantalla principal de ruta, basada en la vista "Navegación Háptica" del mockup:
 * flecha curva gigante, distancia en la esquina y píldora con el destino.
 */
@Composable
fun GuidanceScreen(
    state: GuidanceUiState,
    onRepeat: () -> Unit,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
) {
    // La tarjeta de maniobra (ítem 0) es lo primero que debe verse al levantar la muñeca.
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    VibraBackground {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            ScalingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (!state.hasGuidance) {
                    item(key = "idle") { IdleGuidance(state.connection) }
                    return@ScalingLazyColumn
                }

                item(key = "maneuver") { ManeuverCard(state.maneuver, state.isPaused) }
                item(key = "route") { RouteSummary(state.route) }
                item(key = "paused") {
                    AnimatedVisibility(
                        visible = state.isPaused,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Text(
                            text = stringResource(R.string.guidance_paused_banner),
                            color = VibraColors.Navy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .background(VibraColors.OrangeSoft, RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
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
                        colors = VibraButtons.navy(),
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                stringResource(if (state.isPaused) R.string.action_resume else R.string.action_pause),
                                fontWeight = FontWeight.Bold,
                            )
                        },
                    )
                }
                item(key = "stop") {
                    Button(
                        onClick = onStop,
                        colors = VibraButtons.danger(),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.action_stop), fontWeight = FontWeight.Bold) },
                    )
                }
                state.message?.let { message ->
                    item(key = "message") {
                        Text(
                            text = stringResource(message),
                            color = VibraColors.Error,
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

@Composable
private fun ManeuverCard(maneuver: ManeuverUi?, isPaused: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            // TalkBack anuncia cada indicación nueva sin tocar la pantalla.
            .semantics { liveRegion = LiveRegionMode.Assertive },
    ) {
        NavyCard(
            cornerRadius = 30.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp),
        ) {
            ManeuverIndicator(
                action = maneuver?.action ?: NavigationAction.STRAIGHT,
                eventId = maneuver?.id ?: 0L,
                color = if (maneuver == null || isPaused) VibraColors.CreamMuted else VibraColors.Cream,
                size = 96.dp,
                // Desplazada para que la punta no choque con la distancia de la esquina.
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (-14).dp, y = 6.dp),
            )
            // Distancia en la esquina, como el "3m" del mockup.
            AnimatedContent(
                targetState = maneuver?.distanceMeters,
                transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.7f)) togetherWith fadeOut() },
                label = "distance",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 18.dp),
            ) { meters ->
                if (meters != null) {
                    Text(
                        text = formatDistance(meters.toDouble()),
                        color = VibraColors.Orange,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
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
                color = VibraColors.Navy,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 3,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun RouteSummary(route: ActiveRoute) {
    if (route.currentDestination == null && route.remainingDistanceMeters == null) return
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        route.currentDestination?.let { NavyPill(stringResource(R.string.destination_format, it)) }
        route.remainingDistanceMeters?.let {
            Text(
                text = stringResource(R.string.remaining_label, formatDistance(it)),
                color = VibraColors.NavyMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun IdleGuidance(connection: ConnectionStatus) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        VibraHeader(logoSize = 34.dp)
        Text(
            text = stringResource(R.string.waiting_title),
            color = VibraColors.Navy,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.waiting_body),
            color = VibraColors.NavyMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        if (connection !is ConnectionStatus.Connected) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp),
            ) {
                StatusDot(VibraColors.Error)
                Text(
                    text = " " + stringResource(R.string.status_disconnected),
                    color = VibraColors.Error,
                    fontSize = 12.sp,
                )
            }
        }
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
                    route = ActiveRoute("Pasto", 820.0, routeActive = true),
                    maneuver = ManeuverUi(1L, NavigationAction.TURN_RIGHT, "Gira a la derecha en 3 metros", 3),
                ),
                onRepeat = {},
                onTogglePause = {},
                onStop = {},
            )
        }
    }
}
