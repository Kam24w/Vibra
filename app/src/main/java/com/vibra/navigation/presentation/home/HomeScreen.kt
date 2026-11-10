package com.vibra.navigation.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.vibra.navigation.R
import com.vibra.navigation.data.device.BatteryState
import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.presentation.components.BrandWaves
import com.vibra.navigation.presentation.components.ConnectionIndicator
import com.vibra.navigation.presentation.components.ProgressBar
import com.vibra.navigation.presentation.components.StatusDot
import com.vibra.navigation.presentation.components.VibraBackground
import com.vibra.navigation.presentation.components.VibraButtons
import com.vibra.navigation.presentation.components.formatDistance
import com.vibra.navigation.presentation.components.formatElapsed
import com.vibra.navigation.presentation.components.rememberNow
import com.vibra.navigation.presentation.guidance.ManeuverIndicator
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
 * Dashboard del reloj, centrado en una sola idea: el "núcleo" de navegación.
 * De arriba abajo: estado del enlace, núcleo circular con las ondas del logo
 * (toca para abrir la guía), destino o invitación a iniciar ruta, dos mosaicos
 * (sincronía y batería) y el acceso al test háptico.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenGuidance: () -> Unit,
    onOpenHaptics: () -> Unit,
) {
    // Ítem 1 = núcleo de navegación: lo primero que se ve centrado.
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 1)
    val now = rememberNow()
    VibraBackground {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            ScalingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item(key = "status") { ConnectionIndicator(state.connection) }
                item(key = "hero") { NavigationHero(state.route, onOpenGuidance) }
                item(key = "tiles") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        SyncTile(state.lastSyncAt, now, Modifier.weight(1f))
                        BatteryTile(state.battery, Modifier.weight(1f))
                    }
                }
                item(key = "haptics") {
                    Button(
                        onClick = onOpenHaptics,
                        colors = VibraButtons.brand(),
                        modifier = Modifier.fillMaxWidth(),
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_vibration),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        label = { Text(stringResource(R.string.home_open_haptics), fontWeight = FontWeight.Bold) },
                    )
                }
            }
        }
    }
}

/**
 * Núcleo de navegación. Sin ruta: círculo marino con la flecha en azul claro y
 * ondas tenues fijas. Con ruta: círculo naranja de marca, ondas azul/naranja
 * latiendo y debajo el destino con la distancia restante.
 */
@Composable
private fun NavigationHero(route: ActiveRoute, onClick: () -> Unit) {
    val active = route.routeActive
    val title = route.currentDestination?.takeIf { active } ?: stringResource(R.string.home_no_route)
    val subtitle = if (active) {
        route.remainingDistanceMeters?.let { stringResource(R.string.remaining_label, formatDistance(it)) }
            ?: stringResource(R.string.home_route_active)
    } else {
        stringResource(R.string.home_start_hint)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$title. $subtitle" }
            .padding(vertical = 2.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(116.dp)) {
            BrandWaves(
                modifier = Modifier.size(116.dp),
                waves = 3,
                startFraction = 0.36f,
                travelFraction = 0.13f,
                strokeWidth = 3.5.dp,
                sweep = 64f,
                maxAlpha = if (active) 0.95f else 0.5f,
                animated = active,
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .then(
                        if (active) {
                            Modifier.background(VibraGradients.Highlight)
                        } else {
                            Modifier
                                .background(VibraGradients.Card)
                                .border(1.5.dp, VibraColors.Outline, CircleShape)
                        },
                    ),
            ) {
                ManeuverIndicator(
                    action = NavigationAction.STRAIGHT,
                    eventId = 0L,
                    color = if (active) VibraColors.OnOrange else VibraColors.Blue,
                    size = 44.dp,
                )
            }
        }
        Text(
            text = title,
            color = VibraColors.OnSurface,
            fontSize = if (active) 18.sp else 16.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Text(
            text = subtitle,
            color = if (active) VibraColors.OrangeText else VibraColors.OnSurfaceMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

/** Mosaico pequeño: acento de color arriba, valor grande y etiqueta. */
@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(shape)
            .background(VibraGradients.Card)
            .border(1.dp, VibraColors.Outline.copy(alpha = 0.6f), shape)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" }
            .padding(horizontal = 8.dp, vertical = 9.dp),
    ) {
        accent()
        Text(
            text = value,
            color = VibraColors.OnSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = label,
            color = VibraColors.OnSurfaceMuted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SyncTile(lastSyncAt: Long?, now: Long, modifier: Modifier = Modifier) {
    StatTile(
        value = formatElapsed(lastSyncAt, now),
        label = stringResource(R.string.sync_title),
        modifier = modifier,
    ) {
        StatusDot(if (lastSyncAt == null) VibraColors.Disabled else VibraColors.Emerald, size = 8.dp)
    }
}

@Composable
private fun BatteryTile(battery: BatteryState?, modifier: Modifier = Modifier) {
    StatTile(
        value = battery?.let {
            stringResource(if (it.isCharging) R.string.battery_charging else R.string.battery_percent, it.percent)
        } ?: "—",
        label = stringResource(R.string.battery_short),
        modifier = modifier,
    ) {
        ProgressBar(
            fraction = (battery?.percent ?: 0) / 100f,
            height = 5.dp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
            brush = when {
                battery == null -> SolidColor(VibraColors.Disabled)
                battery.percent <= 15 -> SolidColor(VibraColors.DangerText)
                else -> VibraGradients.Brand
            },
        )
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
