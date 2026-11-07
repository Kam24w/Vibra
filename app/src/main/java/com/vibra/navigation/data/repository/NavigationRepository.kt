package com.vibra.navigation.data.repository

import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.data.model.NavigationEvent
import com.vibra.navigation.data.model.UserActionType
import kotlinx.coroutines.flow.StateFlow

/** Qué hacer con un evento entrante. */
enum class EventDisposition {
    /** Evento nuevo: mostrar y vibrar. */
    NEW,
    /** Ya procesado (mismo timestamp o anterior): ignorar. */
    DUPLICATE,
    /** Llegó tarde (p. ej. encolado durante una desconexión): mostrar sin vibrar. */
    STALE,
}

interface NavigationRepository {
    val activeRoute: StateFlow<ActiveRoute>
    val latestEvent: StateFlow<NavigationEvent?>
    val connectionStatus: StateFlow<ConnectionStatus>

    /** Lee el DataItem actual y el estado de conexión (al abrir la app). */
    suspend fun refresh()

    suspend fun sendUserAction(action: UserActionType): Result<Unit>

    // --- Entrada desde el WearableListenerService ---
    fun onNavigationEvent(event: NavigationEvent): EventDisposition
    fun onActiveRouteChanged(route: ActiveRoute)
    fun onConnectionChanged(status: ConnectionStatus)
}
