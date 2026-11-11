package com.vibra.navigation.haptics

import com.vibra.navigation.data.model.NavigationEvent

/**
 * Lo que la app necesita del motor de vibración. [VibrationManager] es la
 * implementación real; los tests usan una falsa para comprobar qué se reprodujo.
 */
interface HapticPlayer {
    val isAvailable: Boolean

    /** Sin control de amplitud la intensidad no se puede ajustar (solo on/off). */
    val hasAmplitudeControl: Boolean

    /** Vibra una indicación recibida del teléfono con su ritmo. */
    fun vibrate(event: NavigationEvent)

    fun vibrate(preset: HapticPreset)

    /** [fullStrength] ignora la intensidad del usuario (alertas, prueba de motor). */
    fun vibrate(waveform: HapticWaveform, fullStrength: Boolean = false)

    fun cancel()
}
