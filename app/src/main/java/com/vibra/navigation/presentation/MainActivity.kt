package com.vibra.navigation.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
    }
}
