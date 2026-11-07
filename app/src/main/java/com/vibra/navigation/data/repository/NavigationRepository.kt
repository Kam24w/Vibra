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

    /**
     * Estado del enlace con WanderAI. Mientras alguien lo observa (la UI visible),
     * el repositorio vigila la conexión de forma activa; sin observadores no gasta batería.
     */
    val connectionStatus: StateFlow<ConnectionStatus>

    /** Momento (epoch ms) del último intercambio correcto con el teléfono. */
    val lastSyncAt: StateFlow<Long?>

    /** Guía pausada desde el reloj: los eventos se muestran pero no vibran. */
    val guidancePaused: StateFlow<Boolean>

    /** Relee el DataItem de la ruta y el estado de conexión. */
    suspend fun refresh()

    /** Pide una comprobación de conexión sin bloquear (p. ej. desde el servicio). */
    fun requestConnectionRefresh()

    /**
     * Envía una acción con reintentos. Si falla y la acción cambia estado (pausa,
     * reanudar, detener), queda pendiente y se reenvía al reconectar.
     */
    suspend fun sendUserAction(action: UserActionType): Result<Unit>

    fun setGuidancePaused(paused: Boolean)

    // --- Entrada desde el WearableListenerService ---
    fun onNavigationEvent(event: NavigationEvent): EventDisposition
    fun onActiveRouteChanged(route: ActiveRoute)
}
