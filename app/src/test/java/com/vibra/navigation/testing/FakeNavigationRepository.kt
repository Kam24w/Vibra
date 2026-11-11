package com.vibra.navigation.testing

import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.data.model.NavigationEvent
import com.vibra.navigation.data.model.UserActionType
import com.vibra.navigation.data.repository.EventDisposition
import com.vibra.navigation.data.repository.NavigationRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** Repositorio en memoria para tests de ViewModels y controladores. */
class FakeNavigationRepository : NavigationRepository {
    override val activeRoute = MutableStateFlow(ActiveRoute.Inactive)
    override val latestEvent = MutableStateFlow<NavigationEvent?>(null)
    override val connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Unknown)
    override val lastSyncAt = MutableStateFlow<Long?>(null)
    override val guidancePaused = MutableStateFlow(false)

    val sentActions = mutableListOf<UserActionType>()
    var sendResult: Result<Unit> = Result.success(Unit)
    var pingResult: Result<Long> = Result.success(42L)
    var refreshCount = 0

    override suspend fun refresh() {
        refreshCount++
    }

    override fun requestConnectionRefresh() = Unit

    override suspend fun sendUserAction(action: UserActionType): Result<Unit> {
        sentActions += action
        return sendResult
    }

    override fun setGuidancePaused(paused: Boolean) {
        guidancePaused.value = paused
    }

    override suspend fun pingPhone(): Result<Long> = pingResult

    override fun onNavigationEvent(event: NavigationEvent): EventDisposition {
        latestEvent.value = event
        return EventDisposition.NEW
    }

    override fun onActiveRouteChanged(route: ActiveRoute) {
        activeRoute.value = route
    }
}
