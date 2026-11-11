package com.vibra.navigation.service

import android.util.Log
import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.data.parser.NavigationEventParser
import com.vibra.navigation.data.parser.ParseResult
import com.vibra.navigation.data.repository.EventDisposition
import com.vibra.navigation.data.repository.NavigationRepository
import com.vibra.navigation.haptics.HapticPlayer

/**
 * Procesa lo que llega del teléfono, venga por la Data Layer ([VibraListenerService])
 * o por el puente de desarrollo del emulador
 * ([com.vibra.navigation.data.wear.LocalDevBridgeClient]).
 */
class IncomingEventHandler(
    private val parser: NavigationEventParser,
    private val repository: () -> NavigationRepository,
    private val vibrationManager: () -> HapticPlayer,
) {
    fun onNavigationEventPayload(payload: ByteArray) {
        when (val result = parser.parse(payload)) {
            is ParseResult.Failure -> Log.w(TAG, "Evento descartado: ${result.reason}", result.cause)
            is ParseResult.Success -> {
                val event = result.event
                val repository = repository()
                when (repository.onNavigationEvent(event)) {
                    EventDisposition.NEW ->
                        if (!shouldVibrate(event.action, repository.guidancePaused.value)) {
                            Log.i(TAG, "Guía en pausa: ${event.action} sin vibración")
                        } else {
                            vibrationManager().vibrate(event)
                        }
                    EventDisposition.STALE -> Log.i(TAG, "Evento atrasado, se muestra sin vibrar: ${event.action}")
                    EventDisposition.DUPLICATE -> Log.d(TAG, "Evento duplicado ignorado: ${event.timestamp}")
                }
            }
        }
    }

    fun onActiveRoute(route: ActiveRoute) {
        val repository = repository()
        val wasActive = repository.activeRoute.value.routeActive
        repository.onActiveRouteChanged(route)
        if (wasActive && !route.routeActive) vibrationManager().cancel()
    }

    companion object {
        private const val TAG = "VibraListenerService"

        /** Con la guía en pausa solo vibran las alertas: WanderAI las envía a propósito. */
        fun shouldVibrate(action: NavigationAction, guidancePaused: Boolean): Boolean =
            !guidancePaused || action == NavigationAction.ALERT
    }
}
