package com.vibra.navigation.presentation.haptics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.navigation.data.settings.HapticSettingsRepository
import com.vibra.navigation.haptics.HapticPreset
import com.vibra.navigation.haptics.VibrationManager
import com.vibra.navigation.presentation.components.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class HapticTestUiState(
    val intensityLevel: Int = HapticSettingsRepository.DEFAULT_LEVEL,
    val isVibratorAvailable: Boolean = true,
    val hasAmplitudeControl: Boolean = true,
    /** Último patrón reproducido y contador para animar el botón aunque se repita. */
    val lastPlayed: HapticPreset? = null,
    val playCount: Int = 0,
) {
    val minLevel get() = HapticSettingsRepository.MIN_LEVEL
    val maxLevel get() = HapticSettingsRepository.MAX_LEVEL
}

private data class Playback(val preset: HapticPreset? = null, val count: Int = 0)

class HapticTestViewModel(
    private val settings: HapticSettingsRepository,
    private val vibrationManager: VibrationManager,
) : ViewModel() {

    private val playback = MutableStateFlow(Playback())

    val uiState: StateFlow<HapticTestUiState> = combine(settings.intensityLevel, playback) { level, play ->
        HapticTestUiState(
            intensityLevel = level,
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

    /** Cambia la intensidad (se aplica también a la navegación) y la deja sentir. */
    fun changeIntensity(delta: Int) {
        val current = settings.intensityLevel.value
        val next = (current + delta).coerceIn(HapticSettingsRepository.MIN_LEVEL, HapticSettingsRepository.MAX_LEVEL)
        if (next == current) return
        settings.setIntensityLevel(next)
        play(HapticPreset.PULSE)
    }

    override fun onCleared() {
        vibrationManager.cancel()
    }

    companion object {
        val Factory = containerViewModelFactory { c -> HapticTestViewModel(c.hapticSettings, c.vibrationManager) }
    }
}
