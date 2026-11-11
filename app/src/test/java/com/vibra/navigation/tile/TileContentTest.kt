package com.vibra.navigation.tile

import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.ConnectionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class TileContentTest {

    @Test
    fun `an active route opens the guidance`() {
        val content = tileContentFor(ActiveRoute("Plaza", 640.0, routeActive = true), ConnectionStatus.Unknown, paused = true)
        assertEquals(TileContent.Route("Plaza", 640.0, paused = true), content)
        assertEquals(TileAction.OPEN_GUIDANCE, content.action)
    }

    @Test
    fun `without a route the tile opens the app and shows the link`() {
        val linked = tileContentFor(ActiveRoute.Inactive, ConnectionStatus.Connected("moto g"), paused = false)
        assertEquals(TileContent.Idle(phoneLinked = true), linked)
        assertEquals(TileAction.OPEN_APP, linked.action)

        assertEquals(
            TileContent.Idle(phoneLinked = false),
            tileContentFor(ActiveRoute.Inactive, ConnectionStatus.Disconnected, paused = false),
        )
    }
}
