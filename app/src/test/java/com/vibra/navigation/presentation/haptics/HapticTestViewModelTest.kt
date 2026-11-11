package com.vibra.navigation.presentation.haptics

import com.vibra.navigation.haptics.HapticPreset
import com.vibra.navigation.testing.FakeHapticPlayer
import com.vibra.navigation.testing.FakeHapticSettings
import com.vibra.navigation.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HapticTestViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val player = FakeHapticPlayer()
    private val settings = FakeHapticSettings(initialPercent = 60)
    // Diferido: debe crearse después de que la regla sustituya Dispatchers.Main.
    private val viewModel by lazy { HapticTestViewModel(settings, player) }

    @Test
    fun `playing a command vibrates it and counts repeats`() = runTest(mainRule.dispatcher) {
        backgroundScope.launchCollect()

        viewModel.play(HapticPreset.U_TURN)
        viewModel.play(HapticPreset.U_TURN)

        assertEquals(listOf("preset:U_TURN", "preset:U_TURN"), player.played)
        assertEquals(HapticPreset.U_TURN, viewModel.uiState.value.lastPlayed)
        assertEquals(2, viewModel.uiState.value.playCount)
    }

    @Test
    fun `changing the intensity saves it and lets the user feel it`() = runTest(mainRule.dispatcher) {
        backgroundScope.launchCollect()

        viewModel.setIntensity(80)

        assertEquals(80, settings.intensityPercent.value)
        assertEquals(80, viewModel.uiState.value.intensityPercent)
        assertEquals(listOf("preset:PULSE"), player.played)
    }

    @Test
    fun `the same intensity or an out of range one does not vibrate twice`() = runTest(mainRule.dispatcher) {
        viewModel.setIntensity(60)
        assertEquals(emptyList<String>(), player.played)

        viewModel.setIntensity(150)
        assertEquals(100, settings.intensityPercent.value)
    }

    @Test
    fun `motor capabilities come from the player`() = runTest(mainRule.dispatcher) {
        player.isAvailable = false
        player.hasAmplitudeControl = false
        val other = HapticTestViewModel(settings, player)
        backgroundScope.launch { other.uiState.collect {} }

        assertEquals(false, other.uiState.value.isVibratorAvailable)
        assertEquals(false, other.uiState.value.hasAmplitudeControl)
    }

    private fun kotlinx.coroutines.CoroutineScope.launchCollect() = launch { viewModel.uiState.collect {} }
}
