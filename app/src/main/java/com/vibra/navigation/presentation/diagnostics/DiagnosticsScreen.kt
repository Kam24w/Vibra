package com.vibra.navigation.presentation.diagnostics

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
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
import com.vibra.navigation.data.device.DeviceInfo
import com.vibra.navigation.data.model.LinkTransport
import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.presentation.components.StatusDot
import com.vibra.navigation.presentation.components.SurfaceCard
import com.vibra.navigation.presentation.components.VibraBackground
import com.vibra.navigation.presentation.components.VibraButtons
import com.vibra.navigation.presentation.components.formatElapsed
import com.vibra.navigation.presentation.components.rememberNow
import com.vibra.navigation.presentation.theme.VibraColors
import com.vibra.navigation.presentation.theme.VibraTheme

@Composable
fun DiagnosticsRoute(viewModel: DiagnosticsViewModel = viewModel(factory = DiagnosticsViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleStartEffect(Unit) {
        viewModel.refresh()
        onStopOrDispose { }
    }
    DiagnosticsScreen(state = state, onPing = viewModel::pingPhone, onVibrate = viewModel::testVibration)
}

/**
 * Diagnóstico para la prueba en sitio: un veredicto claro ("Listo para la prueba"),
 * lo que falla, los datos del enlace y del reloj, y dos pruebas rápidas.
 */
@Composable
fun DiagnosticsScreen(state: DiagnosticsUiState, onPing: () -> Unit, onVibrate: () -> Unit) {
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
                item(key = "title") {
                    Text(
                        stringResource(R.string.diag_title),
                        color = VibraColors.OnSurfaceMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                item(key = "verdict") { Verdict(state.isReady) }
                state.issues.forEach { issue ->
                    item(key = "issue_${issue.name}") { IssueRow(issue) }
                }
                item(key = "link") {
                    SurfaceCard {
                        InfoRow(R.string.diag_link, stringResource(state.transport.labelRes()))
                        InfoRow(R.string.diag_phone, state.phoneName ?: "—")
                        InfoRow(
                            R.string.diag_last_message,
                            formatElapsed(state.lastSyncAt, now) + (state.lastEventAction?.let { " · ${it.shortName()}" } ?: ""),
                        )
                    }
                }
                item(key = "ping") {
                    Button(
                        onClick = onPing,
                        enabled = state.ping != PingState.Running,
                        colors = VibraButtons.brand(),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.diag_ping), fontWeight = FontWeight.Bold) },
                        secondaryLabel = { Text(state.ping.label(), maxLines = 1) },
                    )
                }
                item(key = "watch") {
                    SurfaceCard {
                        InfoRow(R.string.diag_motor, stringResource(state.motorLabelRes()))
                        state.device?.let { device ->
                            InfoRow(R.string.diag_watch, device.displayName)
                            Text(
                                stringResource(R.string.diag_android, device.androidVersion, device.sdkInt),
                                color = VibraColors.OnSurfaceMuted,
                                fontSize = 11.sp,
                            )
                            Text(stringResource(R.string.diag_app, device.appVersion), color = VibraColors.OnSurfaceMuted, fontSize = 11.sp)
                        }
                    }
                }
                item(key = "vibrate") {
                    Button(
                        onClick = onVibrate,
                        enabled = state.vibratorAvailable,
                        colors = VibraButtons.primary(),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.diag_vibrate), fontWeight = FontWeight.Bold) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Verdict(ready: Boolean) {
    val text = stringResource(if (ready) R.string.diag_ready else R.string.diag_not_ready)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                contentDescription = text
            },
    ) {
        StatusDot(if (ready) VibraColors.Emerald else VibraColors.DangerText, size = 10.dp)
        Spacer(Modifier.width(6.dp))
        Text(text, color = VibraColors.OnSurface, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun IssueRow(issue: DiagnosticIssue) {
    Text(
        text = stringResource(issue.messageRes()),
        color = if (issue.blocking) VibraColors.DangerText else VibraColors.Warning,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
    )
}

@Composable
private fun InfoRow(@StringRes label: Int, value: String) {
    Column(Modifier.padding(vertical = 2.dp)) {
        Text(stringResource(label), color = VibraColors.OnSurfaceMuted, fontSize = 10.sp)
        Text(
            value,
            color = VibraColors.OnSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PingState.label(): String = when (this) {
    PingState.Idle -> stringResource(R.string.diag_ping_idle)
    PingState.Running -> stringResource(R.string.diag_ping_running)
    is PingState.Success -> stringResource(R.string.diag_ping_ok, millis.toInt())
    PingState.Failed -> stringResource(R.string.diag_ping_failed)
}

@StringRes
private fun LinkTransport.labelRes(): Int = when (this) {
    LinkTransport.BLUETOOTH -> R.string.diag_transport_bluetooth
    LinkTransport.CLOUD -> R.string.diag_transport_cloud
    LinkTransport.DEV_BRIDGE -> R.string.diag_transport_bridge
    LinkTransport.NONE -> R.string.diag_transport_none
    LinkTransport.UNAVAILABLE -> R.string.diag_transport_unavailable
}

@StringRes
private fun DiagnosticsUiState.motorLabelRes(): Int = when {
    !vibratorAvailable -> R.string.diag_motor_none
    amplitudeControl -> R.string.diag_motor_amplitude
    else -> R.string.diag_motor_on_off
}

@StringRes
private fun DiagnosticIssue.messageRes(): Int = when (this) {
    DiagnosticIssue.API_UNAVAILABLE -> R.string.diag_issue_api
    DiagnosticIssue.NO_PHONE -> R.string.diag_issue_no_phone
    DiagnosticIssue.NO_VIBRATOR -> R.string.diag_issue_no_vibrator
    DiagnosticIssue.BLUETOOTH_OFF -> R.string.diag_issue_bluetooth_off
    DiagnosticIssue.VIA_CLOUD -> R.string.diag_issue_via_cloud
    DiagnosticIssue.INTENSITY_OFF -> R.string.diag_issue_intensity_off
    DiagnosticIssue.PING_FAILED -> R.string.diag_issue_ping_failed
}

/** Nombre corto de la acción para la fila "Último mensaje" (p. ej. "U_TURN"). */
private fun NavigationAction.shortName(): String = name

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun DiagnosticsPreview() {
    VibraTheme {
        AppScaffold {
            DiagnosticsScreen(
                state = DiagnosticsUiState(
                    transport = LinkTransport.BLUETOOTH,
                    phoneName = "moto g",
                    lastSyncAt = System.currentTimeMillis(),
                    lastEventAction = NavigationAction.TURN_LEFT,
                    device = DeviceInfo("samsung", "SM-L310", "14", 34, "1.0 (1001)", bluetoothEnabled = true),
                    ping = PingState.Success(84),
                ),
                onPing = {},
                onVibrate = {},
            )
        }
    }
}
