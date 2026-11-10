package com.vibra.navigation.haptics

import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.data.model.VibrationPattern
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

        assertNotEquals(left.amplitudes.count { it > 0 }, right.amplitudes.count { it > 0 })
    }

    @Test
    fun `scaling keeps pauses silent and pulses audible`() {
        val wave = HapticWaveform.from(VibrationPattern(pulses = 2, durationMs = 100, amplitude = 200))

        val half = wave.scaled(0.5f)
        assertArrayEquals(intArrayOf(0, 100, 0, 100), half.amplitudes)
        assertArrayEquals(wave.timings, half.timings)

        val tiny = wave.scaled(0.001f)
        assertArrayEquals(intArrayOf(0, 1, 0, 1), tiny.amplitudes)
    }

    @Test
    fun `wave ramps up to full amplitude and back down`() {
        val wave = HapticWaveform.wave(steps = 4, stepMs = 50)

        assertArrayEquals(intArrayOf(64, 128, 191, 255, 191, 128, 64), wave.amplitudes)
        assertEquals(350L, wave.totalDurationMs)
    }

    @Test
    fun `every preset produces a non empty waveform`() {
        HapticPreset.entries.forEach { preset ->
            val wave = preset.waveform()
            assertTrue("$preset vacío", wave.amplitudes.any { it > 0 })
            assertTrue("$preset demasiado largo", wave.totalDurationMs in 1..2_000)
        }
    }

    @Test
    fun `alert default is five short full strength pulses`() {
        val wave = HapticWaveform.defaultFor(NavigationAction.ALERT)
        assertEquals(5, wave.amplitudes.count { it > 0 })
        assertTrue(wave.amplitudes.filter { it > 0 }.all { it == 255 })
    }

    @Test
    fun `the four navigation commands have distinct rhythms`() {
        val commands = listOf(
            NavigationAction.TURN_LEFT,
            NavigationAction.TURN_RIGHT,
            NavigationAction.STRAIGHT,
            NavigationAction.U_TURN,
        ).map(HapticWaveform::defaultFor)

        assertEquals("todos los patrones deben ser distintos", 4, commands.toSet().size)
        commands.forEach { wave ->
            assertTrue(wave.amplitudes.any { it > 0 })
            assertTrue(wave.totalDurationMs in 1..2_000)
        }
    }

    @Test
    fun `straight pulses accelerate with shrinking gaps and growing strength`() {
        val wave = HapticWaveform.straight()
        val gaps = wave.timings.filterIndexed { i, _ -> i > 0 && wave.amplitudes[i] == 0 }
        val strengths = wave.amplitudes.filter { it > 0 }

        assertEquals(3, strengths.size)
        assertTrue(gaps.zipWithNext().all { (a, b) -> b < a })
        assertTrue(strengths.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun `u turn is an intermittent alert of two bursts`() {
        val wave = HapticWaveform.uTurn()
        assertEquals(6, wave.amplitudes.count { it > 0 })
        assertTrue(wave.amplitudes.filter { it > 0 }.all { it == 255 })
    }
}
