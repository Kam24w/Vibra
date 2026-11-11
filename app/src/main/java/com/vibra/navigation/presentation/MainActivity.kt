package com.vibra.navigation.presentation

import android.content.pm.PackageManager
import android.os.Build
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vibra.navigation.data.wear.DevEnvironment
import com.vibra.navigation.presentation.app.VibraApp

/**
 * La Activity solo aloja la UI. La recepción de eventos y la vibración viven en
 * [com.vibra.navigation.service.VibraListenerService], así siguen funcionando con
 * la pantalla apagada o la app cerrada.
 */
class MainActivity : ComponentActivity() {
    /** Cuenta las veces que se pidió abrir la guía (actividad en curso, Tile...). */
    private val guidanceRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val startInGuidance = savedInstanceState == null && intent.wantsGuidance()
        if (startInGuidance) guidanceRequests.intValue++
        setContent {
            VibraApp(
                startInGuidance = startInGuidance,
                guidanceRequests = guidanceRequests.intValue,
            )
        }
        requestLocalNetworkForDevBridge()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.wantsGuidance()) guidanceRequests.intValue++
    }

    private fun Intent.wantsGuidance() = getBooleanExtra(EXTRA_OPEN_GUIDANCE, false)

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

    companion object {
        private const val PERMISSION_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
        private const val EXTRA_OPEN_GUIDANCE = "com.vibra.navigation.OPEN_GUIDANCE"

        /** Abre Vibra directamente en la guía, saltando la presentación. */
        fun openGuidanceIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_GUIDANCE, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
