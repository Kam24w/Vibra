package com.vibra.navigation.haptics

import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.data.model.VibrationPattern
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticWaveformTest {

    @Test
    fun `two pulses produce off-on-off-on waveform`() {
        val wave = HapticWaveform.from(VibrationPattern(pulses = 2, durationMs = 150, amplitude = 255))

        assertArrayEquals(longArrayOf(0, 150, 150, 150), wave.timings)
        assertArrayEquals(intArrayOf(0, 255, 0, 255), wave.amplitudes)
    }

    @Test
    fun `out of range values are clamped`() {
        val wave = HapticWaveform.from(VibrationPattern(pulses = 100, durationMs = 60_000, amplitude = 999))

        assertEquals(HapticWaveform.MAX_PULSES * 2, wave.timings.size)
        assertTrue(wave.timings.all { it <= HapticWaveform.MAX_PULSE_MS })
        assertTrue(wave.amplitudes.all { it in 0..255 })
    }

    @Test
    fun `zero or negative values still produce a valid waveform`() {
        val wave = HapticWaveform.from(VibrationPattern(pulses = 0, durationMs = -1, amplitude = 0))

        assertArrayEquals(longArrayOf(0, HapticWaveform.MIN_PULSE_MS), wave.timings)
        assertArrayEquals(intArrayOf(0, 1), wave.amplitudes)
    }

    @Test
    fun `left and right defaults are distinguishable by pulse count`() {
        val left = HapticWaveform.defaultFor(NavigationAction.TURN_LEFT)
        val right = HapticWaveform.defaultFor(NavigationAction.TURN_RIGHT)

        assertTrue(left.amplitudes.count { it > 0 } != right.amplitudes.count { it > 0 })
    }
}
