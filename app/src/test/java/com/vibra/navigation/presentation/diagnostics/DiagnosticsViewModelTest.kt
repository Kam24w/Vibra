package com.vibra.navigation.presentation.diagnostics

import com.vibra.navigation.data.device.DeviceInfo
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.data.model.LinkTransport
import com.vibra.navigation.testing.FakeHapticPlayer
import com.vibra.navigation.testing.FakeHapticSettings
import com.vibra.navigation.testing.FakeNavigationRepository
import com.vibra.navigation.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DiagnosticsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeNavigationRepository()
    private val player = FakeHapticPlayer()
    private val device = DeviceInfo("samsung", "SM-L310", "14", 34, "1.0 (1001)", bluetoothEnabled = true)
    private val viewModel by lazy {
        DiagnosticsViewModel(repository, FakeHapticSettings(), player) { device }
    }

    @Test
    fun `the link, phone and device are reported`() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }
        repository.connectionStatus.value = ConnectionStatus.Connected("moto g", isNearby = true)

        val state = viewModel.uiState.value
        assertEquals(LinkTransport.BLUETOOTH, state.transport)
        assertEquals("moto g", state.phoneName)
        assertEquals(device, state.device)
        assertTrue(state.isReady)
        assertTrue("opening the screen re-checks the link", repository.refreshCount >= 1)
    }

    @Test
    fun `ping reports the delivery time or the failure`() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { viewModel.uiState.collect {} }

        repository.pingResult = Result.success(84L)
        viewModel.pingPhone()
        assertEquals(PingState.Success(84L), viewModel.uiState.value.ping)

        repository.pingResult = Result.failure(IllegalStateException("sin enlace"))
        viewModel.pingPhone()
        assertEquals(PingState.Failed, viewModel.uiState.value.ping)
        assertTrue(DiagnosticIssue.PING_FAILED in viewModel.uiState.value.issues)
    }

    @Test
    fun `the motor check always vibrates at full strength`() = runTest(mainRule.dispatcher) {
        viewModel.testVibration()
        assertEquals(listOf("waveform:full"), player.played)
    }
}
