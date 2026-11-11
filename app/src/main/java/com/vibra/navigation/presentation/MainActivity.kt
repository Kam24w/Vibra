package com.vibra.navigation.presentation

import android.Manifest
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
        requestRuntimePermissions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.wantsGuidance()) guidanceRequests.intValue++
    }

    private fun Intent.wantsGuidance() = getBooleanExtra(EXTRA_OPEN_GUIDANCE, false)

    /**
     * Pide de una vez los permisos que falten:
     *  - POST_NOTIFICATIONS (Wear OS 4+, API 33): la ruta activa en la esfera.
     *  - ACCESS_LOCAL_NETWORK, solo debug + emulador: desde Android 17 el socket del
     *    puente a 10.0.2.2 lo necesita (declarado solo en `src/debug`).
     */
    private fun requestRuntimePermissions() {
        val missing = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            if (DevEnvironment.localBridgeEnabled && Build.VERSION.SDK_INT >= 37) add(PERMISSION_LOCAL_NETWORK)
        }.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 0)
    }

    companion object {
        private const val PERMISSION_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
        /** Extra booleano: abrir directamente la guía (actividad en curso, Tile). */
        const val EXTRA_OPEN_GUIDANCE = "com.vibra.navigation.OPEN_GUIDANCE"

        /** Abre Vibra directamente en la guía, saltando la presentación. */
        fun openGuidanceIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_GUIDANCE, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
