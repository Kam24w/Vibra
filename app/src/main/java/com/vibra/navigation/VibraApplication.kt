package com.vibra.navigation

import android.app.Application
import com.vibra.navigation.di.AppContainer

class VibraApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // También cuando el sistema despierta el proceso solo para un mensaje de WanderAI.
        container.ongoingGuidance.start(container.applicationScope)
    }
}
