package com.vibra.navigation.presentation.guidance

import com.vibra.navigation.R
import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.data.model.NavigationEvent
import com.vibra.navigation.data.model.UserActionType
import com.vibra.navigation.testing.FakeHapticPlayer
import com.vibra.navigation.testing.FakeNavigationRepository
import com.vibra.navigation.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GuidanceViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeNavigationRepository()
    private val player = FakeHapticPlayer()
    private val viewModel by lazy { GuidanceViewModel(repository, player) }

    @Test
    fun `pause is local and immediate, then resume`() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.togglePause()
        assertTrue(viewModel.uiState.value.isPaused)
        assertEquals(listOf("cancel"), player.played)

        viewModel.togglePause()
        assertFalse(viewModel.uiState.value.isPaused)
        assertEquals(listOf(UserActionType.PAUSE_GUIDANCE, UserActionType.RESUME_GUIDANCE), repository.sentActions)
    }

    @Test
    fun `stop closes the route on the watch and tells the phone`() = runTest(mainRule.dispatcher) {
        repository.activeRoute.value = ActiveRoute("Plaza", 300.0, routeActive = true)

        viewModel.stopNavigation()

        assertFalse(repository.activeRoute.value.routeActive)
        assertEquals(listOf("cancel"), player.played)
        assertEquals(listOf(UserActionType.STOP_NAVIGATION), repository.sentActions)
    }

    @Test
    fun `repeat vibrates the last cue again`() = runTest(mainRule.dispatcher) {
        repository.latestEvent.value = NavigationEvent(NavigationAction.TURN_RIGHT, timestamp = 5L)

        viewModel.repeatInstruction()

        assertEquals(listOf("event:TURN_RIGHT"), player.played)
        assertEquals(listOf(UserActionType.REPEAT_INSTRUCTION), repository.sentActions)
    }

    @Test
    fun `failed sends explain whether the action was queued`() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }
        repository.sendResult = Result.failure(IllegalStateException("sin enlace"))

        viewModel.togglePause()
        assertEquals(R.string.error_send_queued, viewModel.uiState.value.message)

        viewModel.repeatInstruction()
        assertEquals(R.string.error_send_failed, viewModel.uiState.value.message)

        viewModel.dismissMessage()
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `the latest cue is exposed as the maneuver`() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }
        repository.latestEvent.value = NavigationEvent(NavigationAction.U_TURN, "Da la vuelta", 0, timestamp = 9L)

        val maneuver = viewModel.uiState.value.maneuver
        assertEquals(NavigationAction.U_TURN, maneuver?.action)
        assertEquals("Da la vuelta", maneuver?.instruction)
        assertTrue(viewModel.uiState.value.hasGuidance)
    }
}
