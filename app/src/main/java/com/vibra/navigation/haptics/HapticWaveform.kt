package com.vibra.navigation.haptics

import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.data.model.VibrationPattern
import kotlin.math.roundToInt

/**
 * Representación pura (sin Android) de una forma de onda, para poder testearla en JVM.
 * `timings[i]` dura `amplitudes[i]`; amplitud 0 = pausa.
 */
data class HapticWaveform(
    val timings: LongArray,
    val amplitudes: IntArray,
) {
    init {
        require(timings.size == amplitudes.size) { "timings y amplitudes deben tener el mismo tamaño" }
    }

    val totalDurationMs: Long get() = timings.sum()

    /** Aplica la intensidad del usuario (0..1) sin apagar ningún pulso. */
    fun scaled(factor: Float): HapticWaveform {
        val f = factor.coerceIn(0f, 1f)
        return copy(
            amplitudes = IntArray(amplitudes.size) { i ->
                val a = amplitudes[i]
                if (a == 0) 0 else (a * f).roundToInt().coerceIn(1, 255)
            },
        )
    }

    override fun equals(other: Any?): Boolean =
        other is HapticWaveform && timings.contentEquals(other.timings) && amplitudes.contentEquals(other.amplitudes)

    override fun hashCode(): Int = 31 * timings.contentHashCode() + amplitudes.contentHashCode()

    companion object {
        const val MAX_PULSES = 6
        const val MIN_PULSE_MS = 20L
        const val MAX_PULSE_MS = 1_500L
        const val MIN_GAP_MS = 80L

        /**
         * Construye la onda a partir del patrón recibido, recortando valores fuera de
         * rango para que un payload erróneo no deje el motor vibrando segundos.
         */
        fun from(pattern: VibrationPattern): HapticWaveform {
            val pulses = pattern.pulses.coerceIn(1, MAX_PULSES)
            val duration = pattern.durationMs.coerceIn(MIN_PULSE_MS, MAX_PULSE_MS)
            val amplitude = pattern.amplitude.coerceIn(1, 255)
            val gap = duration.coerceAtLeast(MIN_GAP_MS)

            val timings = LongArray(pulses * 2)
            val amplitudes = IntArray(pulses * 2)
            for (i in 0 until pulses) {
                timings[i * 2] = if (i == 0) 0L else gap
                amplitudes[i * 2] = 0
                timings[i * 2 + 1] = duration
                amplitudes[i * 2 + 1] = amplitude
            }
            return HapticWaveform(timings, amplitudes)
        }

        /** Patrón por defecto si WanderAI no envía `vibration_pattern`. */
        fun defaultFor(action: NavigationAction): HapticWaveform = from(
            when (action) {
                NavigationAction.TURN_LEFT -> VibrationPattern(pulses = 2, durationMs = 150, amplitude = 255)
                NavigationAction.TURN_RIGHT -> VibrationPattern(pulses = 3, durationMs = 150, amplitude = 255)
                NavigationAction.SLIGHT_LEFT -> VibrationPattern(pulses = 2, durationMs = 80, amplitude = 180)
                NavigationAction.SLIGHT_RIGHT -> VibrationPattern(pulses = 3, durationMs = 80, amplitude = 180)
                NavigationAction.STRAIGHT -> VibrationPattern(pulses = 1, durationMs = 120, amplitude = 150)
                NavigationAction.U_TURN -> VibrationPattern(pulses = 4, durationMs = 250, amplitude = 255)
                NavigationAction.ARRIVED -> VibrationPattern(pulses = 1, durationMs = 800, amplitude = 255)
                NavigationAction.REROUTING -> VibrationPattern(pulses = 2, durationMs = 400, amplitude = 200)
                // Igual que WanderAI: 5 pulsos cortos a máxima amplitud para que no pase desapercibida.
                NavigationAction.ALERT -> VibrationPattern(pulses = 5, durationMs = 100, amplitude = 255)
                NavigationAction.UNKNOWN -> VibrationPattern(pulses = 1, durationMs = 200, amplitude = 200)
            },
        )

        /** Onda: la amplitud sube y baja de forma progresiva. */
        fun wave(steps: Int = 8, stepMs: Long = 60L): HapticWaveform {
            val up = (1..steps).map { (255f * it / steps).roundToInt() }
            val levels = up + up.reversed().drop(1)
            return HapticWaveform(
                timings = LongArray(levels.size) { stepMs },
                amplitudes = levels.toIntArray(),
            )
        }

        /** Ritmo: tres toques cortos y uno largo. */
        fun rhythm(): HapticWaveform = HapticWaveform(
            timings = longArrayOf(0, 70, 90, 70, 90, 70, 160, 400),
            amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255),
        )

        /** Pulso único para la prueba rápida de VibrationEffect. */
        fun pulse(durationMs: Long = 300L): HapticWaveform =
            HapticWaveform(longArrayOf(0, durationMs), intArrayOf(0, 255))
    }
}

/** Patrones que se pueden sentir desde la pantalla de test háptico. */
enum class HapticPreset {
    LEFT, RIGHT, PULSE, WAVE, RHYTHM;

    fun waveform(): HapticWaveform = when (this) {
        LEFT -> HapticWaveform.defaultFor(NavigationAction.TURN_LEFT)
        RIGHT -> HapticWaveform.defaultFor(NavigationAction.TURN_RIGHT)
        PULSE -> HapticWaveform.pulse()
        WAVE -> HapticWaveform.wave()
        RHYTHM -> HapticWaveform.rhythm()
    }
}
