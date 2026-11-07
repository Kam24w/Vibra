package com.vibra.navigation.di

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.vibra.navigation.data.parser.NavigationEventParser
import com.vibra.navigation.data.repository.NavigationRepository
import com.vibra.navigation.data.repository.WearNavigationRepository
import com.vibra.navigation.haptics.VibrationManager

/**
 * DI manual (sin Hilt para mantener el build ligero). Vive en [com.vibra.navigation.VibraApplication],
 * por lo que Activity, ViewModel y WearableListenerService comparten las mismas instancias.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val parser: NavigationEventParser by lazy { NavigationEventParser() }

    val navigationRepository: NavigationRepository by lazy {
        WearNavigationRepository(
            dataClient = Wearable.getDataClient(appContext),
            messageClient = Wearable.getMessageClient(appContext),
            nodeClient = Wearable.getNodeClient(appContext),
            capabilityClient = Wearable.getCapabilityClient(appContext),
            parser = parser,
        )
    }

    val vibrationManager: VibrationManager by lazy { VibrationManager(appContext) }
}
