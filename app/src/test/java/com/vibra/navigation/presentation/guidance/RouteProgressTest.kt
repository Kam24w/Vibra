package com.vibra.navigation.presentation.guidance

import com.vibra.navigation.data.model.ActiveRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteProgressTest {

    private fun route(destination: String?, remaining: Double?, active: Boolean = true) =
        ActiveRoute(destination, remaining, active)

    private fun RouteProgress.feed(vararg updates: ActiveRoute) = updates.fold(this) { acc, r -> acc.next(r) }

    @Test
    fun `first distance is the baseline and progress grows as it shrinks`() {
        val progress = RouteProgress().feed(route("Plaza", 1_000.0), route("Plaza", 250.0))
        assertEquals(1_000.0, progress.baselineMeters!!, 0.0)
        assertEquals(0.75f, progress.fraction!!, 0.0001f)
    }

    @Test
    fun `starts at zero for a new route`() {
        assertEquals(0f, RouteProgress().next(route("Plaza", 820.0)).fraction!!, 0f)
    }

    @Test
    fun `a longer recalculated route raises the baseline`() {
        val progress = RouteProgress().feed(route("Plaza", 800.0), route("Plaza", 1_200.0), route("Plaza", 600.0))
        assertEquals(0.5f, progress.fraction!!, 0.0001f)
    }

    @Test
    fun `a new destination resets the baseline`() {
        val progress = RouteProgress().feed(route("Plaza", 1_000.0), route("Plaza", 100.0), route("Museo", 400.0))
        assertEquals(400.0, progress.baselineMeters!!, 0.0)
        assertEquals(0f, progress.fraction!!, 0f)
    }

    @Test
    fun `inactive route or unknown distance has no progress`() {
        assertNull(RouteProgress().next(route("Plaza", 500.0, active = false)).fraction)
        assertNull(RouteProgress().next(route("Plaza", null)).fraction)
        assertNull(RouteProgress().feed(route("Plaza", 500.0), ActiveRoute.Inactive).baselineMeters)
    }

    @Test
    fun `missing distance update keeps the baseline of the same route`() {
        val progress = RouteProgress().feed(route("Plaza", 500.0), route("Plaza", null))
        assertEquals(500.0, progress.baselineMeters!!, 0.0)
        assertNull(progress.fraction)
    }
}
