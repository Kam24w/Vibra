package com.vibra.navigation.data.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ajustes hápticos persistentes. Se usa SharedPreferences (un único valor pequeño,
 * lectura síncrona barata) para que el WearableListenerService pueda aplicar la
 * intensidad sin esperar a corrutinas.
 */
class HapticSettingsRepository(context: Context) : HapticSettings {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _intensityPercent = MutableStateFlow(readInitialPercent())

    override val intensityPercent: StateFlow<Int> = _intensityPercent.asStateFlow()

    override val intensityFactor: Float get() = _intensityPercent.value / 100f

    override fun setIntensityPercent(percent: Int) {
        val clamped = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)
        _intensityPercent.value = clamped
        prefs.edit { putInt(KEY_PERCENT, clamped) }
    }

    private fun readInitialPercent(): Int = initialIntensityPercent(
        savedPercent = if (prefs.contains(KEY_PERCENT)) prefs.getInt(KEY_PERCENT, DEFAULT_PERCENT) else null,
        legacyLevel = if (prefs.contains(LEGACY_KEY_LEVEL)) prefs.getInt(LEGACY_KEY_LEVEL, 5) else null,
    )

    companion object {
        /**
         * Intensidad inicial: el porcentaje guardado; si no hay, migra el antiguo nivel
         * 1–5 (20 % por nivel); si tampoco, 100 %. Siempre dentro de 0..100.
         */
        fun initialIntensityPercent(savedPercent: Int?, legacyLevel: Int?): Int =
            (savedPercent ?: legacyLevel?.times(20) ?: DEFAULT_PERCENT).coerceIn(MIN_PERCENT, MAX_PERCENT)

        const val MIN_PERCENT = 0
        const val MAX_PERCENT = 100
        const val STEP_PERCENT = 10
        const val DEFAULT_PERCENT = 100
        private const val PREFS = "vibra_haptics"
        private const val KEY_PERCENT = "intensity_percent"
        private const val LEGACY_KEY_LEVEL = "intensity_level"
    }
}
