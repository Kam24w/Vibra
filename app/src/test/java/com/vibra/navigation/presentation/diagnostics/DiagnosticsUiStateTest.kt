package com.vibra.navigation.presentation.diagnostics

import com.vibra.navigation.data.device.DeviceInfo
import com.vibra.navigation.data.model.LinkTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsUiStateTest {

    private val galaxyWatch = DeviceInfo(
        manufacturer = "samsung",
        model = "SM-L310",
        androidVersion = "14",
        sdkInt = 34,
        appVersion = "1.0 (1001)",
        bluetoothEnabled = true,
    )

    private val ready = DiagnosticsUiState(transport = LinkTransport.BLUETOOTH, phoneName = "moto g", device = galaxyWatch)

    @Test
    fun `a galaxy watch linked by bluetooth is ready with no issues`() {
        assertTrue(ready.isReady)
        assertEquals(emptyList<DiagnosticIssue>(), ready.issues)
        assertEquals("Samsung SM-L310", galaxyWatch.displayName)
    }

    @Test
    fun `no phone blocks the test`() {
        val state = ready.copy(transport = LinkTransport.NONE)
        assertFalse(state.isReady)
        assertEquals(listOf(DiagnosticIssue.NO_PHONE), state.issues)
    }

    @Test
    fun `cloud link, bluetooth off, zero intensity and failed ping are only warnings`() {
        val state = ready.copy(
            transport = LinkTransport.CLOUD,
            device = galaxyWatch.copy(bluetoothEnabled = false),
            intensityPercent = 0,
            ping = PingState.Failed,
        )
        assertTrue(state.isReady)
        assertEquals(
            listOf(
                DiagnosticIssue.VIA_CLOUD,
                DiagnosticIssue.BLUETOOTH_OFF,
                DiagnosticIssue.INTENSITY_OFF,
                DiagnosticIssue.PING_FAILED,
            ),
            state.issues,
        )
    }

    @Test
    fun `blocking issues come first`() {
        val state = ready.copy(transport = LinkTransport.UNAVAILABLE, vibratorAvailable = false, intensityPercent = 0)
        assertEquals(
            listOf(DiagnosticIssue.API_UNAVAILABLE, DiagnosticIssue.NO_VIBRATOR, DiagnosticIssue.INTENSITY_OFF),
            state.issues,
        )
    }

    @Test
    fun `bluetooth state is ignored on the emulator bridge`() {
        val state = ready.copy(transport = LinkTransport.DEV_BRIDGE, device = galaxyWatch.copy(bluetoothEnabled = false))
        assertTrue(state.issues.isEmpty())
    }
}
