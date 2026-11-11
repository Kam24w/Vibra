package com.vibra.navigation.presentation.haptics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.navigation.data.settings.HapticSettingsRepository
import com.vibra.navigation.haptics.HapticPreset
import com.vibra.navigation.haptics.HapticPlayer
import com.vibra.navigation.presentation.components.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class HapticTestUiState(
    val intensityPercent: Int = HapticSettingsRepository.DEFAULT_PERCENT,
    val isVibratorAvailable: Boolean = true,
    val hasAmplitudeControl: Boolean = true,
    /** Último patrón reproducido y contador para animar el botón aunque se repita. */
    val lastPlayed: HapticPreset? = null,
    val playCount: Int = 0,
)

private data class Playback(val preset: HapticPreset? = null, val count: Int = 0)

/** Compartido por el test háptico (pantalla 4) y el ajuste de intensidad (pantalla 5). */
class HapticTestViewModel(
    private val settings: HapticSettingsRepository,
    private val vibrationManager: HapticPlayer,
) : ViewModel() {

    private val playback = MutableStateFlow(Playback())

    val uiState: StateFlow<HapticTestUiState> = combine(settings.intensityPercent, playback) { percent, play ->
        HapticTestUiState(
            intensityPercent = percent,
            isVibratorAvailable = vibrationManager.isAvailable,
            hasAmplitudeControl = vibrationManager.hasAmplitudeControl,
            lastPlayed = play.preset,
            playCount = play.count,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HapticTestUiState())

    fun play(preset: HapticPreset) {
        vibrationManager.vibrate(preset)
        playback.update { Playback(preset, it.count + 1) }
    }

    /** Fija la intensidad (se aplica también a la navegación) y la deja sentir. */
    fun setIntensity(percent: Int) {
        val next = percent.coerceIn(HapticSettingsRepository.MIN_PERCENT, HapticSettingsRepository.MAX_PERCENT)
        if (next == settings.intensityPercent.value) return
        settings.setIntensityPercent(next)
        play(HapticPreset.PULSE)
    }

    override fun onCleared() {
        vibrationManager.cancel()
    }

    companion object {
        val Factory = containerViewModelFactory { c -> HapticTestViewModel(c.hapticSettings, c.vibrationManager) }
    }
}
