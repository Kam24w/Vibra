package com.vibra.navigation.presentation.diagnostics

import com.vibra.navigation.data.device.DeviceInfo
import com.vibra.navigation.data.model.LinkTransport
import com.vibra.navigation.data.model.NavigationAction

/** Resultado de la última prueba de enlace con el teléfono. */
sealed interface PingState {
    data object Idle : PingState
    data object Running : PingState
    data class Success(val millis: Long) : PingState
    data object Failed : PingState
}

/**
 * Problemas que impiden o empeoran la prueba en sitio. Los [blocking] hacen que el
 * reloj no esté listo; el resto son avisos.
 */
enum class DiagnosticIssue(val blocking: Boolean) {
    /** La API de Wearable no existe: no es un reloj Wear OS con Play services. */
    API_UNAVAILABLE(blocking = true),

    /** No hay teléfono con WanderAI alcanzable. */
    NO_PHONE(blocking = true),

    /** El reloj no tiene motor de vibración. */
    NO_VIBRATOR(blocking = true),

    /** El Bluetooth del reloj está apagado. */
    BLUETOOTH_OFF(blocking = false),

    /** Enlazado por Wi-Fi/nube: funciona, pero con más latencia que por Bluetooth. */
    VIA_CLOUD(blocking = false),

    /** La intensidad está al 0 %: solo vibrarán las alertas. */
    INTENSITY_OFF(blocking = false),

    /** El último ping al teléfono falló. */
    PING_FAILED(blocking = false),
}

data class DiagnosticsUiState(
    val transport: LinkTransport = LinkTransport.NONE,
    val phoneName: String? = null,
    val lastSyncAt: Long? = null,
    val lastEventAction: NavigationAction? = null,
    val routeActive: Boolean = false,
    val device: DeviceInfo? = null,
    val vibratorAvailable: Boolean = true,
    val amplitudeControl: Boolean = true,
    val intensityPercent: Int = 100,
    val ping: PingState = PingState.Idle,
) {
    val issues: List<DiagnosticIssue> get() = diagnose(this)

    /** Listo para la prueba: ningún problema bloqueante. */
    val isReady: Boolean get() = issues.none { it.blocking }
}

/** Lista ordenada (bloqueantes primero) de lo que falla o conviene revisar. */
fun diagnose(state: DiagnosticsUiState): List<DiagnosticIssue> = buildList {
    when (state.transport) {
        LinkTransport.UNAVAILABLE -> add(DiagnosticIssue.API_UNAVAILABLE)
        LinkTransport.NONE -> add(DiagnosticIssue.NO_PHONE)
        LinkTransport.CLOUD -> add(DiagnosticIssue.VIA_CLOUD)
        LinkTransport.BLUETOOTH, LinkTransport.DEV_BRIDGE -> Unit
    }
    if (!state.vibratorAvailable) add(DiagnosticIssue.NO_VIBRATOR)
    // En el emulador (puente ADB) el Bluetooth no importa.
    if (state.device?.bluetoothEnabled == false && state.transport != LinkTransport.DEV_BRIDGE) {
        add(DiagnosticIssue.BLUETOOTH_OFF)
    }
    if (state.intensityPercent == 0) add(DiagnosticIssue.INTENSITY_OFF)
    if (state.ping == PingState.Failed) add(DiagnosticIssue.PING_FAILED)
}.sortedByDescending { it.blocking }
