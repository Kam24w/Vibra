package com.vibra.navigation.ongoing

import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.data.model.NavigationEvent

/**
 * Lo que muestra la actividad en curso de Vibra mientras hay una ruta: destino,
 * distancia restante, última maniobra y si la guía está en pausa. Puro, para tests.
 */
data class OngoingGuidance(
    val destination: String?,
    val remainingMeters: Double?,
    val lastAction: NavigationAction?,
    val paused: Boolean,
)

/** null cuando no hay ruta activa: entonces la actividad en curso se retira. */
fun ongoingGuidanceFor(route: ActiveRoute, latestEvent: NavigationEvent?, paused: Boolean): OngoingGuidance? {
    if (!route.routeActive) return null
    return OngoingGuidance(
        destination = route.currentDestination,
        remainingMeters = route.remainingDistanceMeters,
        // Las alertas no son una maniobra: no deben quedar como "última indicación".
        lastAction = latestEvent?.action?.takeIf { it != NavigationAction.ALERT && it != NavigationAction.UNKNOWN },
        paused = paused,
    )
}
