package com.vibra.navigation

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NavigationEngineTest {

    private lateinit var navigationEngine: NavigationEngine

    @Before
    fun setUp() {
        navigationEngine = NavigationEngine()
    }

    @Test
    fun `small angle difference returns straight`() {
        val result = navigationEngine.calculateDirection(
            currentBearing = 0.0,
            targetBearing = 20.0
        )

        assertEquals(Direction.STRAIGHT, result)
    }

    @Test
    fun `positive angle difference returns right`() {
        val result = navigationEngine.calculateDirection(
            currentBearing = 0.0,
            targetBearing = 90.0
        )

        assertEquals(Direction.RIGHT, result)
    }

    @Test
    fun `negative angle difference returns left`() {
        val result = navigationEngine.calculateDirection(
            currentBearing = 90.0,
            targetBearing = 0.0
        )

        assertEquals(Direction.LEFT, result)
    }

    @Test
    fun `opposite bearing returns back`() {
        val result = navigationEngine.calculateDirection(
            currentBearing = 0.0,
            targetBearing = 180.0
        )

        assertEquals(Direction.BACK, result)
    }

    @Test
    fun `crossing north with small angle difference returns straight`() {
        val result = navigationEngine.calculateDirection(
            currentBearing = 350.0,
            targetBearing = 10.0
        )

        assertEquals(Direction.STRAIGHT, result)
    }

    @Test
    fun `angle difference of 30 degrees returns straight`() {
        val result = navigationEngine.calculateDirection(
            currentBearing = 0.0,
            targetBearing = 30.0
        )

        assertEquals(Direction.STRAIGHT, result)
    }
    @Test
    fun `angle difference of 150 degrees returns back`() {
        val result = navigationEngine.calculateDirection(
            currentBearing = 0.0,
            targetBearing = 150.0
        )

        assertEquals(Direction.BACK, result)
    }
}

