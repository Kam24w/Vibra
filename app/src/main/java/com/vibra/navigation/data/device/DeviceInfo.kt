package com.vibra.navigation.data.device

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import com.vibra.navigation.BuildConfig

/** Datos del reloj que ayudan a diagnosticar una prueba en sitio (p. ej. "SM-L310 · Android 14"). */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val appVersion: String,
    /** null si el reloj no tiene Bluetooth o no se pudo leer. */
    val bluetoothEnabled: Boolean?,
) {
    val displayName: String get() = "${manufacturer.replaceFirstChar { it.uppercase() }} $model"
}

/** Lee [DeviceInfo] del sistema; la lectura es barata y no requiere permisos peligrosos. */
class DeviceInfoReader(context: Context) {
    private val appContext = context.applicationContext

    fun read(): DeviceInfo = DeviceInfo(
        manufacturer = Build.MANUFACTURER.orEmpty(),
        model = Build.MODEL.orEmpty(),
        androidVersion = Build.VERSION.RELEASE.orEmpty(),
        sdkInt = Build.VERSION.SDK_INT,
        appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        bluetoothEnabled = runCatching {
            appContext.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled
        }.getOrNull(),
    )
}
