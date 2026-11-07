package com.vibra.navigation.haptics

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.vibra.navigation.data.model.NavigationEvent

/**
 * Gestor háptico con compatibilidad entre versiones:
 *  - API 31+: [VibratorManager.getDefaultVibrator]
 *  - API 30:  [Vibrator] directo (deprecado en 31)
 *  - API 33+: [VibrationAttributes]; antes, [AudioAttributes].
 *
 * Se usa la categoría ALARM porque Android ignora las vibraciones de apps en
 * background con usos "touch"/"unknown", y el WearableListenerService no es un
 * proceso en primer plano. ALARM además atraviesa el modo No Molestar, que es lo
 * deseado para indicaciones de navegación críticas.
 *
 * @param intensity factor 0..1 elegido por el usuario (ver HapticSettingsRepository).
 */
class VibrationManager(
    context: Context,
    private val intensity: () -> Float = { 1f },
) {

    private val vibrator: Vibrator? = resolveVibrator(context.applicationContext)

    val isAvailable: Boolean get() = vibrator?.hasVibrator() == true

    /** Sin control de amplitud la intensidad no se puede ajustar (solo on/off). */
    val hasAmplitudeControl: Boolean get() = vibrator?.hasAmplitudeControl() == true

    fun vibrate(event: NavigationEvent) {
        val waveform = event.vibrationPattern?.let(HapticWaveform::from)
            ?: HapticWaveform.defaultFor(event.action)
        vibrate(waveform)
    }

    fun vibrate(preset: HapticPreset) = vibrate(preset.waveform())

    fun vibrate(waveform: HapticWaveform) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: run {
            Log.w(TAG, "Dispositivo sin vibrador")
            return
        }
        try {
            // Cancela un patrón anterior para que la nueva indicación no se solape.
            vibrator.cancel()
            val effect = waveform.scaled(intensity()).toEffect(vibrator.hasAmplitudeControl())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrateApi33(vibrator, effect)
            } else {
                vibrateLegacy(vibrator, effect)
            }
        } catch (e: RuntimeException) {
            // SecurityException (falta VIBRATE) o IllegalArgumentException (onda inválida).
            Log.e(TAG, "No se pudo vibrar", e)
        }
    }

    fun cancel() {
        vibrator?.cancel()
    }

    private fun HapticWaveform.toEffect(hasAmplitudeControl: Boolean): VibrationEffect =
        if (hasAmplitudeControl) {
            VibrationEffect.createWaveform(timings, amplitudes, NO_REPEAT)
        } else {
            // Sin amplitud: los tramos con amplitud > 0 vibran, el resto son pausas.
            VibrationEffect.createWaveform(onOffTimings(), NO_REPEAT)
        }

    /** Convierte la onda a la forma alterna pausa/vibración que exige la API sin amplitud. */
    private fun HapticWaveform.onOffTimings(): LongArray {
        val result = mutableListOf<Long>()
        var on = false
        var acc = 0L
        for (i in timings.indices) {
            val segmentOn = amplitudes[i] > 0
            if (segmentOn != on) {
                result += acc
                acc = 0L
                on = segmentOn
            }
            acc += timings[i]
        }
        result += acc
        return result.toLongArray()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun vibrateApi33(vibrator: Vibrator, effect: VibrationEffect) {
        vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
    }

    @Suppress("DEPRECATION")
    private fun vibrateLegacy(vibrator: Vibrator, effect: VibrationEffect) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        vibrator.vibrate(effect, attrs)
    }

    private companion object {
        const val TAG = "VibrationManager"
        const val NO_REPEAT = -1

        fun resolveVibrator(context: Context): Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
    }
}
