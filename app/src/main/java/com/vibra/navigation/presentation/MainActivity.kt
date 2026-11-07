package com.vibra.navigation.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import com.vibra.navigation.presentation.guidance.GuidanceRoute
import com.vibra.navigation.presentation.theme.VibraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            VibraTheme {
                AppScaffold(timeText = { TimeText() }) { GuidanceRoute() }
            }
        }
    }
}
