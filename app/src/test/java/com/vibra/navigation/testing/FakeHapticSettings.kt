package com.vibra.navigation.testing

import com.vibra.navigation.data.settings.HapticSettings
import kotlinx.coroutines.flow.MutableStateFlow

/** Ajustes hápticos en memoria. */
class FakeHapticSettings(initialPercent: Int = 100) : HapticSettings {
    override val intensityPercent = MutableStateFlow(initialPercent)
    override val intensityFactor: Float get() = intensityPercent.value / 100f

    override fun setIntensityPercent(percent: Int) {
        intensityPercent.value = percent.coerceIn(0, 100)
    }
}
