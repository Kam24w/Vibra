package com.vibra.navigation.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vibra.navigation.R
import com.vibra.navigation.presentation.haptics.HapticTestUiState
import com.vibra.navigation.presentation.haptics.IntensityScreen
import com.vibra.navigation.presentation.theme.VibraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntensityScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun showsThePercentageAndItsLevel() {
        compose.setContent { VibraTheme { IntensityScreen(HapticTestUiState(intensityPercent = 70), {}, {}) } }
        compose.onNodeWithText(context.getString(R.string.battery_percent, 70)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.intensity_medium)).assertIsDisplayed()
    }

    @Test
    fun zeroPercentWarnsThatVibrationIsOff() {
        compose.setContent { VibraTheme { IntensityScreen(HapticTestUiState(intensityPercent = 0), {}, {}) } }
        compose.onNodeWithText(context.getString(R.string.intensity_off)).assertIsDisplayed()
    }
}
