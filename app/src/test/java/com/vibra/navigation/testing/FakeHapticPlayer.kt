package com.vibra.navigation.testing

import com.vibra.navigation.data.model.NavigationEvent
import com.vibra.navigation.haptics.HapticPlayer
import com.vibra.navigation.haptics.HapticPreset
import com.vibra.navigation.haptics.HapticWaveform

/** Motor de vibración falso: registra lo que se reprodujo, en orden. */
class FakeHapticPlayer(
    override var isAvailable: Boolean = true,
    override var hasAmplitudeControl: Boolean = true,
) : HapticPlayer {
    val played = mutableListOf<String>()

    override fun vibrate(event: NavigationEvent) {
        played += "event:${event.action}"
    }

    override fun vibrate(preset: HapticPreset) {
        played += "preset:$preset"
    }

    override fun vibrate(waveform: HapticWaveform, fullStrength: Boolean) {
        played += if (fullStrength) "waveform:full" else "waveform"
    }

    override fun cancel() {
        played += "cancel"
    }
}
