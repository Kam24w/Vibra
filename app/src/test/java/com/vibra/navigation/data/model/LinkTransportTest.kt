package com.vibra.navigation.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkTransportTest {

    @Test
    fun `a nearby node is a bluetooth link`() {
        assertEquals(LinkTransport.BLUETOOTH, ConnectionStatus.Connected("Moto g", isNearby = true).transport())
    }

    @Test
    fun `a node that is not nearby goes through the cloud`() {
        assertEquals(LinkTransport.CLOUD, ConnectionStatus.Connected("Moto g", isNearby = false).transport())
    }

    @Test
    fun `the dev bridge wins over the nearby flag`() {
        val status = ConnectionStatus.Connected("WanderAI (puente ADB)", isNearby = true, viaDevBridge = true)
        assertEquals(LinkTransport.DEV_BRIDGE, status.transport())
    }

    @Test
    fun `no phone and no api are told apart`() {
        assertEquals(LinkTransport.NONE, ConnectionStatus.Disconnected.transport())
        assertEquals(LinkTransport.NONE, ConnectionStatus.Unknown.transport())
        assertEquals(LinkTransport.UNAVAILABLE, ConnectionStatus.Unavailable("sin Play services").transport())
    }
}
