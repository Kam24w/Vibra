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
class HapticSettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _intensityLevel = MutableStateFlow(
        prefs.getInt(KEY_INTENSITY, DEFAULT_LEVEL).coerceIn(MIN_LEVEL, MAX_LEVEL),
    )

    /** Nivel de intensidad de [MIN_LEVEL] a [MAX_LEVEL]. */
    val intensityLevel: StateFlow<Int> = _intensityLevel.asStateFlow()

    /** Factor de amplitud 0.2–1.0 correspondiente al nivel actual. */
    val intensityFactor: Float get() = _intensityLevel.value / MAX_LEVEL.toFloat()

    fun setIntensityLevel(level: Int) {
        val clamped = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        _intensityLevel.value = clamped
        prefs.edit { putInt(KEY_INTENSITY, clamped) }
    }

    companion object {
        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 5
        const val DEFAULT_LEVEL = 5
        private const val PREFS = "vibra_haptics"
        private const val KEY_INTENSITY = "intensity_level"
    }
}
