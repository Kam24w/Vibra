package com.vibra.navigation.presentation.guidance

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.navigation.R
import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.data.model.NavigationEvent
import com.vibra.navigation.data.model.UserActionType
import com.vibra.navigation.data.repository.NavigationRepository
import com.vibra.navigation.haptics.VibrationManager
import com.vibra.navigation.presentation.components.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado inmutable de la pantalla de navegación háptica. */
data class GuidanceUiState(
    val connection: ConnectionStatus = ConnectionStatus.Unknown,
    val route: ActiveRoute = ActiveRoute.Inactive,
    /** 0..1 recorrido de la ruta; null si aún no se puede calcular. */
    val routeProgress: Float? = null,
    val maneuver: ManeuverUi? = null,
    val isPaused: Boolean = false,
    val isSending: Boolean = false,
    @param:StringRes val message: Int? = null,
) {
    val hasGuidance: Boolean get() = route.routeActive || maneuver != null
    val isAlert: Boolean get() = maneuver?.action == NavigationAction.ALERT
}

/**
 * Progreso de la ruta a partir de la distancia restante: la primera distancia (o la
 * mayor, si el teléfono recalcula a una ruta más larga) de cada destino es la base.
 */
data class RouteProgress(
    val route: ActiveRoute = ActiveRoute.Inactive,
    val baselineMeters: Double? = null,
) {
    val fraction: Float?
        get() {
            val base = baselineMeters?.takeIf { it > 0 } ?: return null
            val remaining = route.remainingDistanceMeters ?: return null
            return (1 - remaining / base).toFloat().coerceIn(0f, 1f)
        }

    fun next(update: ActiveRoute): RouteProgress {
        if (!update.routeActive) return RouteProgress(update, null)
        val remaining = update.remainingDistanceMeters
        val sameRoute = route.routeActive && route.currentDestination == update.currentDestination
        val baseline = when {
            remaining == null -> baselineMeters.takeIf { sameRoute }
            !sameRoute || baselineMeters == null || remaining > baselineMeters -> remaining
            else -> baselineMeters
        }
        return RouteProgress(update, baseline)
    }
}

data class ManeuverUi(
    /** Identidad del evento: cambia en cada indicación y dispara las animaciones. */
    val id: Long,
    val action: NavigationAction,
    val instruction: String,
    val distanceMeters: Int?,
)

private data class LocalState(val isSending: Boolean = false, @param:StringRes val message: Int? = null)

class GuidanceViewModel(
    private val repository: NavigationRepository,
    private val vibrationManager: VibrationManager,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<GuidanceUiState> = combine(
        repository.connectionStatus,
        repository.activeRoute.scan(RouteProgress(), RouteProgress::next),
        repository.latestEvent,
        repository.guidancePaused,
        localState,
    ) { connection, progress, event, paused, local ->
        GuidanceUiState(
            connection = connection,
            route = progress.route,
            routeProgress = progress.fraction,
            maneuver = event?.toUi(),
            isPaused = paused,
            isSending = local.isSending,
            message = local.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GuidanceUiState())

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    /** Vuelve a vibrar la última indicación en el reloj y avisa a WanderAI. */
    fun repeatInstruction() {
        repository.latestEvent.value?.let(vibrationManager::vibrate)
        send(UserActionType.REPEAT_INSTRUCTION)
    }

    /**
     * Pausa/reanuda la guía. El cambio es local e inmediato (el reloj deja de vibrar
     * aunque no haya enlace) y se notifica a WanderAI; si no llega, se reenvía al reconectar.
     */
    fun togglePause() {
        val pause = !repository.guidancePaused.value
        repository.setGuidancePaused(pause)
        if (pause) vibrationManager.cancel()
        send(if (pause) UserActionType.PAUSE_GUIDANCE else UserActionType.RESUME_GUIDANCE)
    }

    /**
     * Detiene la ruta: el reloj la cierra al instante y avisa a WanderAI, que cancela la
     * ruta en el teléfono. Si no hay enlace, STOP_NAVIGATION queda pendiente y se reenvía.
     */
    fun stopNavigation() {
        vibrationManager.cancel()
        repository.onActiveRouteChanged(ActiveRoute.Inactive)
        send(UserActionType.STOP_NAVIGATION)
    }

    fun dismissMessage() = localState.update { it.copy(message = null) }

    private fun send(action: UserActionType) {
        localState.update { it.copy(isSending = true, message = null) }
        viewModelScope.launch {
            val result = repository.sendUserAction(action)
            localState.update {
                it.copy(
                    isSending = false,
                    message = when {
                        result.isSuccess -> null
                        action == UserActionType.REPEAT_INSTRUCTION -> R.string.error_send_failed
                        else -> R.string.error_send_queued
                    },
                )
            }
        }
    }

    companion object {
        val Factory = containerViewModelFactory { c ->
            GuidanceViewModel(c.navigationRepository, c.vibrationManager)
        }
    }
}

private fun NavigationEvent.toUi() = ManeuverUi(
    id = timestamp,
    action = action,
    instruction = instruction,
    distanceMeters = distanceMeters,
)
