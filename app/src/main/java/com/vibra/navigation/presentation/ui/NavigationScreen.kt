package com.vibra.navigation.presentation.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.vibra.navigation.R
import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.presentation.ManeuverUi
import com.vibra.navigation.presentation.NavigationUiState
import com.vibra.navigation.presentation.theme.VibraColors
import com.vibra.navigation.presentation.theme.VibraTheme

/**
 * Pantalla sin estado: recibe [NavigationUiState] y callbacks. Esto la hace
 * previsualizable y testeable sin ViewModel.
 *
 * Diseño basado en design/diseño.png (pantalla "Navegación Háptica"): tarjeta con
 * la maniobra y la distancia, y píldora con el destino debajo.
 */
@Composable
fun NavigationScreen(
    state: NavigationUiState,
    onRepeat: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
) {
    val listState = rememberScalingLazyListState()

    // ScreenScaffold coordina TimeText (definido en AppScaffold) y el ScrollIndicator
    // con el scroll de la lista, y aplica el padding seguro para pantallas redondas.
    ScreenScaffold(scrollState = listState) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxWidth(),
        ) {
            item(key = "status") { ConnectionLabel(state.connection) }

            if (state.route.routeActive || state.maneuver != null) {
                item(key = "maneuver") { ManeuverCard(state.maneuver) }
                item(key = "route") { RouteSummary(state.route) }
                item(key = "repeat") {
                    Button(
                        onClick = onRepeat,
                        enabled = state.maneuver != null,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.action_repeat), fontWeight = FontWeight.Bold) },
                    )
                }
                item(key = "stop") {
                    FilledTonalButton(
                        onClick = onStop,
                        enabled = state.canSendActions,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.action_stop)) },
                    )
                }
            } else {
                item(key = "idle") { IdleBlock() }
            }

            if (state.errorMessage != null || state.connection !is ConnectionStatus.Connected) {
                item(key = "error") { ErrorBlock(state.errorMessage, onRetry) }
            }
        }
    }
}

@Composable
private fun ConnectionLabel(status: ConnectionStatus) {
    val (text, targetColor) = when (status) {
        is ConnectionStatus.Connected -> stringResource(R.string.status_connected, status.nodeName) to VibraColors.Success
        ConnectionStatus.Disconnected -> stringResource(R.string.status_disconnected) to VibraColors.Error
        ConnectionStatus.Unknown -> stringResource(R.string.status_searching) to VibraColors.Muted
        is ConnectionStatus.Unavailable -> stringResource(R.string.status_unavailable) to VibraColors.Error
    }
    val color by animateColorAsState(targetColor, label = "connectionColor")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ManeuverCard(maneuver: ManeuverUi?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            // TalkBack anuncia cada indicación nueva sin que el usuario toque la pantalla.
            .semantics { liveRegion = LiveRegionMode.Assertive },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .background(VibraColors.NavyPanel, RoundedCornerShape(28.dp)),
        ) {
            ManeuverIndicator(
                action = maneuver?.action ?: NavigationAction.STRAIGHT,
                eventId = maneuver?.id ?: 0L,
                color = if (maneuver == null) VibraColors.Muted else VibraColors.Cream,
                size = 84.dp,
                modifier = Modifier.align(Alignment.Center),
            )
            // Distancia en la esquina, como en el mockup ("3m").
            AnimatedContent(
                targetState = maneuver?.distanceMeters,
                transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith fadeOut() },
                label = "distance",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 16.dp),
            ) { meters ->
                if (meters != null) {
                    Text(
                        text = formatDistance(meters.toDouble()),
                        color = VibraColors.Orange,
                        fontSize = 24.sp,
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
                color = VibraColors.Cream,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
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
        route.currentDestination?.let {
            // Píldora crema con texto marino, como "Destino: Pasto" en el mockup.
            Text(
                text = stringResource(R.string.destination_format, it),
                color = VibraColors.Navy,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .background(VibraColors.Cream, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        route.remainingDistanceMeters?.let {
            Text(
                text = stringResource(R.string.remaining_label, formatDistance(it)),
                color = VibraColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun IdleBlock() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // El marino del logo se pierde sobre negro: va sobre un círculo crema.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(VibraColors.Cream, CircleShape),
            ) {
                Image(
                    painter = painterResource(R.drawable.vibra_logo),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.app_name),
                color = VibraColors.Cream,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Text(
            text = stringResource(R.string.waiting_title),
            style = MaterialTheme.typography.titleMedium,
            color = VibraColors.Orange,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.waiting_body),
            style = MaterialTheme.typography.bodySmall,
            color = VibraColors.Muted,
            textAlign = TextAlign.Center,
        )
    }
}

/** Mensaje de error opcional + botón de reintento. */
@Composable
private fun ErrorBlock(@StringRes errorMessage: Int?, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        errorMessage?.let {
            Text(
                text = stringResource(it),
                color = VibraColors.Error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
        FilledTonalButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.action_retry)) },
        )
    }
}

@Composable
private fun formatDistance(meters: Double): String =
    if (meters < 1_000) {
        stringResource(R.string.distance_meters, meters.toInt())
    } else {
        stringResource(R.string.distance_km, meters / 1_000)
    }

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun NavigationScreenPreview() {
    VibraTheme {
        AppScaffold {
            NavigationScreen(
                state = NavigationUiState(
                    connection = ConnectionStatus.Connected("Pixel"),
                    route = ActiveRoute("Pasto", 820.0, routeActive = true),
                    maneuver = ManeuverUi(1L, NavigationAction.TURN_RIGHT, "Gira a la derecha en 3 metros", 3),
                ),
                onRepeat = {}, onStop = {}, onRetry = {},
            )
        }
    }
}

@WearPreviewDevices
@Composable
private fun IdlePreview() {
    VibraTheme {
        AppScaffold {
            NavigationScreen(NavigationUiState(connection = ConnectionStatus.Unknown), {}, {}, {})
        }
    }
}
