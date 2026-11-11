package com.vibra.navigation.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maniobra enviada por WanderAI. Cualquier valor desconocido se mapea a [UNKNOWN]
 * (ver `coerceInputValues` en el parser) para que una versión nueva del teléfono
 * no rompa un reloj con una versión anterior.
 */
@Serializable
enum class NavigationAction {
    @SerialName("TURN_LEFT") TURN_LEFT,
    @SerialName("TURN_RIGHT") TURN_RIGHT,
    @SerialName("SLIGHT_LEFT") SLIGHT_LEFT,
    @SerialName("SLIGHT_RIGHT") SLIGHT_RIGHT,
    @SerialName("STRAIGHT") STRAIGHT,
    @SerialName("U_TURN") U_TURN,
    @SerialName("ARRIVED") ARRIVED,
    @SerialName("REROUTING") REROUTING,
    /** Aviso urgente del teléfono (p. ej. "Buscar mi Vibra"): vibra incluso con la guía en pausa. */
    @SerialName("ALERT") ALERT,
    UNKNOWN,
}

/** Payload de `/wanderai/navigation_event`. */
@Serializable
data class NavigationEvent(
    val action: NavigationAction = NavigationAction.UNKNOWN,
    val instruction: String = "",
    @SerialName("distance_meters") val distanceMeters: Int? = null,
    @SerialName("vibration_pattern") val vibrationPattern: VibrationPattern? = null,
    val timestamp: Long = 0L,
)

@Serializable
data class VibrationPattern(
    val pulses: Int = 1,
    @SerialName("duration_ms") val durationMs: Long = 200L,
    /** 1..255. Si el dispositivo no tiene control de amplitud se ignora. */
    val amplitude: Int = 255,
)

/** Estado sincronizado vía DataClient en `/wanderai/active_route`. */
data class ActiveRoute(
    val currentDestination: String?,
    val remainingDistanceMeters: Double?,
    val routeActive: Boolean,
) {
    companion object {
        val Inactive = ActiveRoute(currentDestination = null, remainingDistanceMeters = null, routeActive = false)
    }
}

/** Payload de `/vibra/user_action` (Vibra ➔ WanderAI). */
@Serializable
data class UserAction(
    val action: UserActionType,
    val timestamp: Long,
)

@Serializable
enum class UserActionType {
    @SerialName("REPEAT_INSTRUCTION") REPEAT_INSTRUCTION,
    @SerialName("PAUSE_GUIDANCE") PAUSE_GUIDANCE,
    @SerialName("RESUME_GUIDANCE") RESUME_GUIDANCE,
    @SerialName("STOP_NAVIGATION") STOP_NAVIGATION,
    @SerialName("ACKNOWLEDGE") ACKNOWLEDGE,
}

sealed interface ConnectionStatus {
    data object Unknown : ConnectionStatus
    data object Disconnected : ConnectionStatus

    /**
     * @param isNearby true si el nodo está enlazado directamente por Bluetooth;
     * false si la Data Layer llega por Wi-Fi/nube.
     */
    data class Connected(
        val nodeName: String,
        val isNearby: Boolean = true,
        /** True cuando el enlace es el puente ADB de depuración (emulador), no la Data Layer. */
        val viaDevBridge: Boolean = false,
    ) : ConnectionStatus

    /** La API de Wearable no está disponible (p. ej. falta Google Play Services / emulador sin emparejar). */
    data class Unavailable(val reason: String) : ConnectionStatus
}
