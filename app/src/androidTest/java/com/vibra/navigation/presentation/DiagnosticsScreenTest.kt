package com.vibra.navigation.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vibra.navigation.R
import com.vibra.navigation.data.model.LinkTransport
import com.vibra.navigation.presentation.diagnostics.DiagnosticsScreen
import com.vibra.navigation.presentation.diagnostics.DiagnosticsUiState
import com.vibra.navigation.presentation.theme.VibraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiagnosticsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun aBluetoothLinkShowsReady() {
        compose.setContent {
            VibraTheme {
                DiagnosticsScreen(
                    state = DiagnosticsUiState(transport = LinkTransport.BLUETOOTH, phoneName = "moto g"),
                    onPing = {},
                    onVibrate = {},
                )
            }
        }
        compose.onNodeWithText(text(R.string.diag_ready)).assertIsDisplayed()
    }

    @Test
    fun noPhoneShowsTheBlockingIssue() {
        compose.setContent {
            VibraTheme {
                DiagnosticsScreen(state = DiagnosticsUiState(transport = LinkTransport.NONE), onPing = {}, onVibrate = {})
            }
        }
        compose.onNodeWithText(text(R.string.diag_not_ready)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.diag_issue_no_phone)).assertIsDisplayed()
    }
}
