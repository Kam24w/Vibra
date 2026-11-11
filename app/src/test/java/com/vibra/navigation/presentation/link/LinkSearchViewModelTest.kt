package com.vibra.navigation.presentation.link

import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.testing.FakeNavigationRepository
import com.vibra.navigation.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LinkSearchViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val repository = FakeNavigationRepository()

    @Test
    fun `it keeps looking for the phone every few seconds until it is connected`() = runTest(mainRule.dispatcher) {
        LinkSearchViewModel(repository)
        runCurrent()
        assertEquals(1, repository.refreshCount)

        advanceTimeBy(3_001)
        assertEquals(2, repository.refreshCount)

        repository.connectionStatus.value = ConnectionStatus.Connected("moto g")
        advanceTimeBy(10_000)
        assertEquals("no more retries once linked", 2, repository.refreshCount)
    }
}
