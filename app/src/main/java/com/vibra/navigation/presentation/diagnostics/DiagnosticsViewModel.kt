package com.vibra.navigation.presentation.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.navigation.data.device.DeviceInfo
import com.vibra.navigation.data.device.DeviceInfoReader
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.data.model.transport
import com.vibra.navigation.data.repository.NavigationRepository
import com.vibra.navigation.data.settings.HapticSettingsRepository
import com.vibra.navigation.haptics.HapticWaveform
import com.vibra.navigation.haptics.VibrationManager
import com.vibra.navigation.presentation.components.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Diagnóstico del enlace con WanderAI para la prueba en sitio: transporte usado,
 * teléfono, último mensaje, motor de vibración y una prueba de ida al teléfono.
 */
class DiagnosticsViewModel(
    private val repository: NavigationRepository,
    private val settings: HapticSettingsRepository,
    private val vibrationManager: VibrationManager,
    private val deviceInfoReader: DeviceInfoReader,
) : ViewModel() {

    private val device = MutableStateFlow<DeviceInfo?>(null)
    private val ping = MutableStateFlow<PingState>(PingState.Idle)

    val uiState: StateFlow<DiagnosticsUiState> = combine(
        combine(repository.connectionStatus, repository.lastSyncAt, ::Pair),
        combine(repository.latestEvent, repository.activeRoute, ::Pair),
        settings.intensityPercent,
        device,
        ping,
    ) { (connection, lastSync), (event, route), intensity, info, pingState ->
        DiagnosticsUiState(
            transport = connection.transport(),
            phoneName = (connection as? ConnectionStatus.Connected)?.nodeName,
            lastSyncAt = lastSync,
            lastEventAction = event?.action,
            routeActive = route.routeActive,
            device = info,
            vibratorAvailable = vibrationManager.isAvailable,
            amplitudeControl = vibrationManager.hasAmplitudeControl,
            intensityPercent = intensity,
            ping = pingState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiagnosticsUiState())

    init {
        refresh()
    }

    /** Relee el dispositivo y fuerza una comprobación del enlace. */
    fun refresh() {
        device.value = deviceInfoReader.read()
        viewModelScope.launch { repository.refresh() }
    }

    /** Mide cuánto tarda un mensaje en llegar a WanderAI. */
    fun pingPhone() {
        if (ping.value == PingState.Running) return
        ping.value = PingState.Running
        viewModelScope.launch {
            ping.value = repository.pingPhone().fold(
                onSuccess = { PingState.Success(it) },
                onFailure = { PingState.Failed },
            )
        }
    }

    /** Vibra a máxima fuerza (como una alerta) para comprobar el motor sin depender del ajuste. */
    fun testVibration() {
        vibrationManager.vibrate(HapticWaveform.pulse(), fullStrength = true)
    }

    companion object {
        val Factory = containerViewModelFactory { c ->
            DiagnosticsViewModel(c.navigationRepository, c.hapticSettings, c.vibrationManager, c.deviceInfoReader)
        }
    }
}
