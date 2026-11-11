package com.vibra.navigation.data.model

/**
 * Por dónde llega WanderAI al reloj. Sirve para diagnosticar una prueba en sitio:
 * con un reloj físico lo esperado es [BLUETOOTH]; [CLOUD] significa que la Data
 * Layer está usando Wi-Fi/datos porque el Bluetooth no alcanza.
 */
enum class LinkTransport {
    /** Data Layer con el teléfono enlazado directamente por Bluetooth. */
    BLUETOOTH,

    /** Data Layer a través de Wi-Fi o la nube (teléfono lejos o Bluetooth apagado). */
    CLOUD,

    /** Puente ADB de depuración (solo emulador en builds debug). */
    DEV_BRIDGE,

    /** Sin teléfono alcanzable. */
    NONE,

    /** La API de Wearable no está disponible en este dispositivo. */
    UNAVAILABLE,
}

/** Clasifica el estado de conexión en el transporte que realmente se está usando. */
fun ConnectionStatus.transport(): LinkTransport = when (this) {
    is ConnectionStatus.Connected -> when {
        viaDevBridge -> LinkTransport.DEV_BRIDGE
        isNearby -> LinkTransport.BLUETOOTH
        else -> LinkTransport.CLOUD
    }
    is ConnectionStatus.Unavailable -> LinkTransport.UNAVAILABLE
    ConnectionStatus.Disconnected, ConnectionStatus.Unknown -> LinkTransport.NONE
}
