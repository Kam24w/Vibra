package com.vibra.navigation.presentation

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vibra.navigation.data.wear.DevEnvironment
import com.vibra.navigation.presentation.app.VibraApp

/**
 * La Activity solo aloja la UI. La recepción de eventos y la vibración viven en
 * [com.vibra.navigation.service.VibraListenerService], así siguen funcionando con
 * la pantalla apagada o la app cerrada.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent { VibraApp() }
        requestLocalNetworkForDevBridge()
    }

    /**
     * Solo debug + emulador: desde Android 17 el socket del puente a 10.0.2.2 necesita
     * ACCESS_LOCAL_NETWORK (declarado solo en `src/debug`). En un reloj físico o en
     * release no se pide nada.
     */
    private fun requestLocalNetworkForDevBridge() {
        if (!DevEnvironment.localBridgeEnabled || Build.VERSION.SDK_INT < 37) return
        if (checkSelfPermission(PERMISSION_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(PERMISSION_LOCAL_NETWORK), 0)
    }

    private companion object {
        const val PERMISSION_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}
