package com.vibra.navigation.data.repository

import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.wear.LocalDevBridgeClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalDevBridgeClientTest {

    @Test
    fun `decodes the route sent by WanderAI`() {
        val route = LocalDevBridgeClient.decodeActiveRoute("true\u001FCatedral\u001F1.4 km".toByteArray())
        assertEquals(ActiveRoute("Catedral", 1_400.0, true), route)
    }

    @Test
    fun `blank destination becomes null`() {
        val route = LocalDevBridgeClient.decodeActiveRoute("false\u001F\u001F".toByteArray())
        assertEquals(ActiveRoute(null, null, false), route)
    }

    @Test
    fun `malformed payload is ignored`() {
        assertNull(LocalDevBridgeClient.decodeActiveRoute("garbage".toByteArray()))
        assertNull(LocalDevBridgeClient.decodeActiveRoute("yes\u001FA\u001F1 m".toByteArray()))
    }
}
