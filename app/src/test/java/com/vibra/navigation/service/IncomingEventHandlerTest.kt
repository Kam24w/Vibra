package com.vibra.navigation.service

import com.vibra.navigation.data.model.NavigationAction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingEventHandlerTest {

    @Test
    fun `every cue vibrates while guidance is active`() {
        NavigationAction.entries.forEach { action ->
            assertTrue(IncomingEventHandler.shouldVibrate(action, guidancePaused = false))
        }
    }

    @Test
    fun `only alerts vibrate while guidance is paused`() {
        assertTrue(IncomingEventHandler.shouldVibrate(NavigationAction.ALERT, guidancePaused = true))
        NavigationAction.entries.filter { it != NavigationAction.ALERT }.forEach { action ->
            assertFalse(IncomingEventHandler.shouldVibrate(action, guidancePaused = true))
        }
    }
}
