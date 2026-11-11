package com.vibra.navigation.ongoing

import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.data.model.NavigationEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OngoingGuidanceTest {

    private val route = ActiveRoute("La Casita del Cuy", 820.0, routeActive = true)

    @Test
    fun `no active route means no ongoing activity`() {
        assertNull(ongoingGuidanceFor(ActiveRoute.Inactive, null, paused = false))
    }

    @Test
    fun `an active route shows destination, distance and last maneuver`() {
        val event = NavigationEvent(action = NavigationAction.TURN_LEFT, timestamp = 1L)
        assertEquals(
            OngoingGuidance("La Casita del Cuy", 820.0, NavigationAction.TURN_LEFT, paused = false),
            ongoingGuidanceFor(route, event, paused = false),
        )
    }

    @Test
    fun `alerts are not kept as the last maneuver`() {
        val alert = NavigationEvent(action = NavigationAction.ALERT, timestamp = 1L)
        assertNull(ongoingGuidanceFor(route, alert, paused = false)?.lastAction)
    }

    @Test
    fun `pause is reflected`() {
        assertEquals(true, ongoingGuidanceFor(route, null, paused = true)?.paused)
    }
}
