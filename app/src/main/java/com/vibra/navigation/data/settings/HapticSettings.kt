package com.vibra.navigation.data.settings

import kotlinx.coroutines.flow.StateFlow

/** Intensidad háptica elegida por el usuario; [HapticSettingsRepository] la persiste. */
interface HapticSettings {
    /** Fuerza del motor de 0 % (apagado) a 100 %. */
    val intensityPercent: StateFlow<Int>

    /** Factor de amplitud 0..1 correspondiente al porcentaje actual. */
    val intensityFactor: Float

    fun setIntensityPercent(percent: Int)
}
