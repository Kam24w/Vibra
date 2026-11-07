package com.vibra.navigation.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistanceParsingTest {
    @Test
    fun `parses the formatted distance sent by WanderAI`() {
        assertEquals(850.0, parseDistanceMeters("850 m")!!, 0.001)
        assertEquals(1_400.0, parseDistanceMeters("1.4 km")!!, 0.001)
        assertEquals(1_400.0, parseDistanceMeters("1,4 km")!!, 0.001)
        assertEquals(120.0, parseDistanceMeters("120")!!, 0.001)
    }

    @Test
    fun `unknown text yields null`() {
        assertNull(parseDistanceMeters(""))
        assertNull(parseDistanceMeters("cerca"))
    }
}
