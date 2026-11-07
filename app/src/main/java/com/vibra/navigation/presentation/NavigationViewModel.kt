package com.vibra.navigation.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vibra.navigation.R
import com.vibra.navigation.VibraApplication
import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.data.model.NavigationEvent
import com.vibra.navigation.data.model.UserActionType
import com.vibra.navigation.data.repository.NavigationRepository
import com.vibra.navigation.haptics.VibrationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado inmutable que consume la UI. */
data class NavigationUiState(
    val connection: ConnectionStatus = ConnectionStatus.Unknown,
    val route: ActiveRoute = ActiveRoute.Inactive,
    val maneuver: ManeuverUi? = null,
    val isSending: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
) {
    val canSendActions: Boolean get() = connection is ConnectionStatus.Connected && !isSending
}

data class ManeuverUi(
    /** Identidad del evento: cambia en cada indicación y dispara las animaciones. */
    val id: Long,
    val action: NavigationAction,
    val instruction: String,
    val distanceMeters: Int?,
)

private data class LocalState(val isSending: Boolean = false, @param:StringRes val errorMessage: Int? = null)

class NavigationViewModel(
    private val repository: NavigationRepository,
    private val vibrationManager: VibrationManager?,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<NavigationUiState> = combine(
        repository.connectionStatus,
        repository.activeRoute,
        repository.latestEvent,
        localState,
    ) { connection, route, event, local ->
        NavigationUiState(
            connection = connection,
            route = route,
            maneuver = event?.toUi(),
            isSending = local.isSending,
            errorMessage = local.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        // Mantiene el flujo 5 s tras perder suscriptores (rotación / cambios de config).
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NavigationUiState(),
    )

    /** Se llama cuando la pantalla pasa a visible. */
    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    fun repeatInstruction() {
        repository.latestEvent.value?.let { vibrationManager?.vibrate(it) }
        send(UserActionType.REPEAT_INSTRUCTION)
    }

    fun stopNavigation() = send(UserActionType.STOP_NAVIGATION)

    fun dismissError() = localState.update { it.copy(errorMessage = null) }

    private fun send(action: UserActionType) {
        if (localState.value.isSending) return
        localState.update { it.copy(isSending = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.sendUserAction(action)
            localState.update {
                it.copy(
                    isSending = false,
                    errorMessage = if (result.isFailure) R.string.error_send_failed else null,
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as VibraApplication).container
                NavigationViewModel(container.navigationRepository, container.vibrationManager)
            }
        }
    }
}

private fun NavigationEvent.toUi() = ManeuverUi(
    id = timestamp,
    action = action,
    instruction = instruction,
    distanceMeters = distanceMeters,
)
