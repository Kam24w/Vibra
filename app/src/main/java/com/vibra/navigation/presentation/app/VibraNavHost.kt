package com.vibra.navigation.presentation.app

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.vibra.navigation.presentation.guidance.GuidanceRoute
import com.vibra.navigation.presentation.haptics.HapticTestRoute
import com.vibra.navigation.presentation.home.HomeRoute
import com.vibra.navigation.presentation.splash.SplashScreen
import com.vibra.navigation.presentation.theme.VibraTheme

object VibraDestinations {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val GUIDANCE = "guidance"
    const val HAPTICS = "haptics"
}

/**
 * Grafo de pantallas del reloj. Se navega con deslizar-para-volver (gesto estándar
 * de Wear OS); AppScaffold mantiene el TimeText común a todas las pantallas.
 */
@Composable
fun VibraApp() {
    VibraTheme {
        val navController = rememberSwipeDismissableNavController()
        AppScaffold(timeText = { TimeText() }) {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = VibraDestinations.SPLASH,
            ) {
                composable(VibraDestinations.SPLASH) {
                    SplashScreen(
                        onFinished = {
                            navController.navigate(VibraDestinations.HOME) {
                                popUpTo(VibraDestinations.SPLASH) { inclusive = true }
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
                    )
                }
                composable(VibraDestinations.GUIDANCE) { GuidanceRoute() }
                composable(VibraDestinations.HAPTICS) { HapticTestRoute() }
            }
        }
    }
}
