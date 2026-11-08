package com.vibra.navigation.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.vibra.navigation.data.device.BatteryState
import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.presentation.components.ConnectionIndicator
import com.vibra.navigation.presentation.components.ProgressBar
import com.vibra.navigation.presentation.components.StatusDot
import com.vibra.navigation.presentation.components.SurfaceCard
import com.vibra.navigation.presentation.components.VibraBackground
import com.vibra.navigation.presentation.components.VibraButtons
import com.vibra.navigation.presentation.components.formatDistance
import com.vibra.navigation.presentation.components.formatElapsed
import com.vibra.navigation.presentation.components.rememberNow
import com.vibra.navigation.presentation.theme.VibraColors
import com.vibra.navigation.presentation.theme.VibraGradients
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

/**
 * Dashboard del reloj. La hora curva la pone el AppScaffold; debajo, de arriba abajo:
 * estado del enlace (punto de color), tarjeta de navegación destacada, tarjeta de
 * sincronización + batería y acceso al test háptico. ScalingLazyColumn escala los
 * elementos hacia los bordes para que nada se corte en pantallas redondas.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenGuidance: () -> Unit,
    onOpenHaptics: () -> Unit,
) {
    // Ítem 1 = tarjeta de navegación: es lo primero que se ve centrado.
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 1)
    val now = rememberNow()
    VibraBackground {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            ScalingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item(key = "status") { ConnectionIndicator(state.connection) }
                item(key = "navigation") { NavigationCard(state.route, onOpenGuidance) }
                item(key = "sync_battery") { SyncBatteryCard(state.lastSyncAt, now, state.battery) }
                item(key = "haptics") {
                    Button(
                        onClick = onOpenHaptics,
                        colors = VibraButtons.secondary(),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.home_open_haptics), fontWeight = FontWeight.Bold) },
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta-píldora de navegación. Con ruta activa es naranja con texto negro (alto
 * contraste, el elemento con más peso de la pantalla); sin ruta queda en pizarra.
 */
@Composable
private fun NavigationCard(route: ActiveRoute, onClick: () -> Unit) {
    val active = route.routeActive
    val shape = RoundedCornerShape(32.dp)
    val titleColor = if (active) VibraColors.OnOrange else VibraColors.OnSurface
    val bodyColor = if (active) VibraColors.OnOrange else VibraColors.OnSurfaceMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (active) Modifier.background(VibraGradients.Highlight) else Modifier.background(VibraColors.Surface),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(if (active) R.string.home_route_active else R.string.home_open_guidance),
            color = bodyColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = route.currentDestination?.takeIf { active } ?: stringResource(R.string.home_no_route),
            color = titleColor,
            fontSize = if (active) 18.sp else 15.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (active) {
            route.remainingDistanceMeters?.let {
                Text(
                    text = stringResource(R.string.remaining_label, formatDistance(it)),
                    color = bodyColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Última sincronización y batería háptica, juntas y compactas. */
@Composable
private fun SyncBatteryCard(lastSyncAt: Long?, now: Long, battery: BatteryState?) {
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            StatusDot(if (lastSyncAt == null) VibraColors.Disabled else VibraColors.Emerald, size = 6.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.sync_title),
                color = VibraColors.OnSurfaceMuted,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatElapsed(lastSyncAt, now),
                color = VibraColors.OnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.battery_label),
                color = VibraColors.OnSurfaceMuted,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = battery?.let {
                    stringResource(if (it.isCharging) R.string.battery_charging else R.string.battery_percent, it.percent)
                } ?: "—",
                color = VibraColors.OnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(Modifier.padding(top = 4.dp)) {
            ProgressBar(
                fraction = (battery?.percent ?: 0) / 100f,
                color = when {
                    battery == null -> VibraColors.Disabled
                    battery.percent <= 15 -> VibraColors.DangerText
                    else -> VibraColors.Emerald
                },
            )
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
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

@WearPreviewDevices
@Composable
private fun HomeDisconnectedPreview() {
    VibraTheme {
        AppScaffold {
            HomeScreen(
                state = HomeUiState(connection = ConnectionStatus.Disconnected),
                onOpenGuidance = {},
                onOpenHaptics = {},
            )
        }
    }
}
