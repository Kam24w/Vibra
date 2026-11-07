package com.vibra.navigation.data.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

data class BatteryState(val percent: Int, val isCharging: Boolean)

/**
 * Batería del reloj (el dispositivo háptico). El receiver solo está registrado
 * mientras alguien recolecta el flujo, es decir, con la pantalla visible.
 */
class BatteryMonitor(context: Context) {
    private val appContext = context.applicationContext

    val state: Flow<BatteryState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                intent.toBatteryState()?.let { trySend(it) }
            }
        }
        // ACTION_BATTERY_CHANGED es sticky: el registro devuelve el último valor al instante.
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )?.toBatteryState()?.let { trySend(it) }

        awaitClose { appContext.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    private fun Intent.toBatteryState(): BatteryState? {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return BatteryState(
            percent = level * 100 / scale,
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
        )
    }
}
