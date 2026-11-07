package com.vibra.navigation.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibra.navigation.data.device.BatteryMonitor
import com.vibra.navigation.data.device.BatteryState
import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.data.repository.NavigationRepository
import com.vibra.navigation.presentation.components.containerViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val connection: ConnectionStatus = ConnectionStatus.Unknown,
    val battery: BatteryState? = null,
    val lastSyncAt: Long? = null,
    val route: ActiveRoute = ActiveRoute.Inactive,
)

class HomeViewModel(
    private val repository: NavigationRepository,
    batteryMonitor: BatteryMonitor,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.connectionStatus,
        batteryMonitor.state.onStart<BatteryState?> { emit(null) },
        repository.lastSyncAt,
        repository.activeRoute,
    ) { connection, battery, lastSync, route ->
        HomeUiState(connection, battery, lastSync, route)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    companion object {
        val Factory = containerViewModelFactory { c -> HomeViewModel(c.navigationRepository, c.batteryMonitor) }
    }
}
