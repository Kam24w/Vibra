package com.vibra.navigation.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vibra.navigation.R
import com.vibra.navigation.haptics.HapticPreset
import com.vibra.navigation.presentation.haptics.HapticTestScreen
import com.vibra.navigation.presentation.haptics.HapticTestUiState
import com.vibra.navigation.presentation.theme.VibraTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HapticTestScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun tappingACommandPlaysItsRhythm() {
        val played = mutableListOf<HapticPreset>()
        compose.setContent { VibraTheme { HapticTestScreen(HapticTestUiState(), { played += it }, {}) } }

        compose.onNodeWithContentDescription(context.getString(R.string.haptics_left), substring = true).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.haptics_right), substring = true).performClick()

        assertEquals(listOf(HapticPreset.LEFT, HapticPreset.RIGHT), played)
    }
}
