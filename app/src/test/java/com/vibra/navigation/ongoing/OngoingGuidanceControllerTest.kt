package com.vibra.navigation.ongoing

import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.testing.FakeNavigationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OngoingGuidanceControllerTest {

    private val repository = FakeNavigationRepository()
    private val log = mutableListOf<String>()
    private val controller = OngoingGuidanceController(
        repository = repository,
        show = { log += "show:${it.destination}:${it.remainingMeters}:${it.paused}" },
        cancel = { log += "cancel" },
    )

    @Test
    fun `the ongoing activity follows the route lifecycle`() = runTest(UnconfinedTestDispatcher()) {
        controller.start(backgroundScope)

        repository.activeRoute.value = ActiveRoute("Plaza", 500.0, routeActive = true)
        repository.activeRoute.value = ActiveRoute("Plaza", 300.0, routeActive = true)
        repository.guidancePaused.value = true
        repository.activeRoute.value = ActiveRoute.Inactive

        assertEquals(
            listOf("cancel", "show:Plaza:500.0:false", "show:Plaza:300.0:false", "show:Plaza:300.0:true", "cancel"),
            log,
        )
    }

    @Test
    fun `starting twice does not duplicate updates`() = runTest(UnconfinedTestDispatcher()) {
        controller.start(backgroundScope)
        controller.start(backgroundScope)

        repository.activeRoute.value = ActiveRoute("Plaza", 500.0, routeActive = true)

        assertEquals(listOf("cancel", "show:Plaza:500.0:false"), log)
    }
}
