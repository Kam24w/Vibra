package com.vibra.navigation

import android.app.Application
import com.vibra.navigation.di.AppContainer

class VibraApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
