package com.vibra.navigation.tile

import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.data.model.NavigationEvent
import com.vibra.navigation.testing.FakeNavigationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TileRefresherTest {

    private val repository = FakeNavigationRepository()
    private var updates = 0
    private val refresher = TileRefresher(repository) { updates++ }

    @Test
    fun `only visible changes refresh the tile`() = runTest(UnconfinedTestDispatcher()) {
        refresher.start(backgroundScope)
        assertEquals("the initial state is not pushed", 0, updates)

        repository.activeRoute.value = ActiveRoute("Plaza", 500.0, routeActive = true)
        assertEquals(1, updates)

        // A new cue does not change what the tile shows.
        repository.latestEvent.value = NavigationEvent(NavigationAction.TURN_LEFT, timestamp = 1L)
        assertEquals(1, updates)

        repository.activeRoute.value = ActiveRoute("Plaza", 400.0, routeActive = true)
        repository.guidancePaused.value = true
        assertEquals(3, updates)
    }

    @Test
    fun `linking the phone refreshes the idle tile`() = runTest(UnconfinedTestDispatcher()) {
        refresher.start(backgroundScope)
        repository.connectionStatus.value = ConnectionStatus.Connected("moto g")
        assertEquals(1, updates)
    }
}
