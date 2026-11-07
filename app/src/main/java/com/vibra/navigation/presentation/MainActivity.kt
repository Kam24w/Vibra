package com.vibra.navigation.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import com.vibra.navigation.presentation.theme.VibraTheme
import com.vibra.navigation.presentation.ui.NavigationScreen

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

@Composable
fun VibraApp(viewModel: NavigationViewModel = viewModel(factory = NavigationViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Sincroniza DataItem + conexión cada vez que la pantalla vuelve a ser visible
    // (no hay polling: el resto de cambios llegan vía WearableListenerService).
    LifecycleStartEffect(Unit) {
        viewModel.refresh()
        onStopOrDispose { }
    }

    VibraTheme {
        AppScaffold(timeText = { TimeText() }) {
            NavigationScreen(
                state = state,
                onRepeat = viewModel::repeatInstruction,
                onStop = viewModel::stopNavigation,
                onRetry = {
                    viewModel.dismissError()
                    viewModel.refresh()
                },
            )
        }
    }
}
