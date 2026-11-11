package com.vibra.navigation.presentation.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibra.navigation.presentation.ambient.AmbientGuidance
import com.vibra.navigation.presentation.ambient.LocalAmbientMode
import com.vibra.navigation.presentation.guidance.GuidanceViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.timeTextCurvedText
import com.vibra.navigation.presentation.theme.VibraColors
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.vibra.navigation.presentation.diagnostics.DiagnosticsRoute
import com.vibra.navigation.presentation.guidance.GuidanceRoute
import com.vibra.navigation.presentation.haptics.HapticTestRoute
import com.vibra.navigation.presentation.haptics.IntensityRoute
import com.vibra.navigation.presentation.link.LinkSearchRoute
import com.vibra.navigation.presentation.home.HomeRoute
import com.vibra.navigation.presentation.splash.SplashScreen
import com.vibra.navigation.presentation.theme.VibraTheme

object VibraDestinations {
    const val SPLASH = "splash"
    const val LINK = "link"
    const val HOME = "home"
    const val GUIDANCE = "guidance"
    const val HAPTICS = "haptics"
    const val INTENSITY = "intensity"
    const val DIAGNOSTICS = "diagnostics"
}

/**
 * Grafo de pantallas del reloj. Se navega con deslizar-para-volver (gesto estándar
 * de Wear OS); AppScaffold mantiene el TimeText común a todas las pantallas.
 */
@Composable
fun VibraApp(startInGuidance: Boolean = false, guidanceRequests: Int = 0) {
    VibraTheme {
        val navController = rememberSwipeDismissableNavController()
        // Actividad en curso o Tile: ir a la guía (con el inicio debajo para volver).
        LaunchedEffect(guidanceRequests) {
            if (guidanceRequests > 0) {
                navController.navigate(VibraDestinations.GUIDANCE) { launchSingleTop = true }
            }
        }
        AppScaffold(
            timeText = {
                // Hora en marino sobre el marfil, sin banda de fondo.
                TimeText(backgroundColor = Color.Transparent) { time ->
                    timeTextCurvedText(
                        time,
                        CurvedTextStyle(color = VibraColors.NavyInk, fontWeight = FontWeight.Bold),
                    )
                }
            },
        ) {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = if (startInGuidance) VibraDestinations.HOME else VibraDestinations.SPLASH,
            ) {
                composable(VibraDestinations.SPLASH) {
                    SplashScreen(
                        onStart = {
                            navController.navigate(VibraDestinations.LINK) {
                                popUpTo(VibraDestinations.SPLASH) { inclusive = true }
                            }
                        },
                    )
                }
                composable(VibraDestinations.LINK) {
                    LinkSearchRoute(
                        onLinked = {
                            navController.navigate(VibraDestinations.HOME) {
                                popUpTo(VibraDestinations.LINK) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(VibraDestinations.HOME) {
                    HomeRoute(
                        onOpenGuidance = {
                            navController.navigate(VibraDestinations.GUIDANCE) { launchSingleTop = true }
                        },
                        onOpenHaptics = {
                            navController.navigate(VibraDestinations.HAPTICS) { launchSingleTop = true }
                        },
                        onOpenDiagnostics = {
                            navController.navigate(VibraDestinations.DIAGNOSTICS) { launchSingleTop = true }
                        },
                    )
                }
                composable(VibraDestinations.GUIDANCE) { GuidanceRoute() }
                composable(VibraDestinations.HAPTICS) {
                    HapticTestRoute(
                        onOpenIntensity = {
                            navController.navigate(VibraDestinations.INTENSITY) { launchSingleTop = true }
                        },
                    )
                }
                composable(VibraDestinations.INTENSITY) { IntensityRoute() }
                composable(VibraDestinations.DIAGNOSTICS) { DiagnosticsRoute() }
            }
        }
        // Encima de todo: al atenuarse la pantalla se ve la guía en bajo consumo, sin
        // perder la pantalla en la que estaba el usuario al volver.
        if (LocalAmbientMode.current) {
            val guidance: GuidanceViewModel = viewModel(factory = GuidanceViewModel.Factory)
            val state by guidance.uiState.collectAsStateWithLifecycle()
            AmbientGuidance(state)
        }
    }
}
