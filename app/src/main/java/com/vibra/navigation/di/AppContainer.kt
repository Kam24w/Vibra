package com.vibra.navigation.di

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.vibra.navigation.data.device.BatteryMonitor
import com.vibra.navigation.data.parser.NavigationEventParser
import com.vibra.navigation.data.repository.NavigationRepository
import com.vibra.navigation.data.repository.WearNavigationRepository
import com.vibra.navigation.data.settings.HapticSettingsRepository
import com.vibra.navigation.data.wear.DataLayerContract
import com.vibra.navigation.data.wear.DevEnvironment
import com.vibra.navigation.data.wear.LocalDevBridgeClient
import com.vibra.navigation.haptics.VibrationManager
import com.vibra.navigation.service.IncomingEventHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * DI manual (sin Hilt para mantener el build ligero). Vive en [com.vibra.navigation.VibraApplication],
 * por lo que Activity, ViewModels y WearableListenerService comparten las mismas instancias.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Ámbito de proceso para trabajo que no pertenece a una pantalla (reconexiones, reenvíos). */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val parser: NavigationEventParser by lazy { NavigationEventParser() }

    val navigationRepository: NavigationRepository by lazy {
        WearNavigationRepository(
            dataClient = Wearable.getDataClient(appContext),
            messageClient = Wearable.getMessageClient(appContext),
            nodeClient = Wearable.getNodeClient(appContext),
            capabilityClient = Wearable.getCapabilityClient(appContext),
            parser = parser,
            scope = applicationScope,
            devBridge = devBridge,
        )
    }

    /** Lógica común a la Data Layer y al puente de desarrollo. */
    val incomingEventHandler: IncomingEventHandler by lazy {
        IncomingEventHandler(parser, { navigationRepository }, { vibrationManager })
    }

    /**
     * Solo emulador + debug: puente por túnel ADB para cuando Play services no empareja
     * ("Got 0 accounts"). En un reloj físico es null y solo se usa la Data Layer.
     */
    private val devBridge: LocalDevBridgeClient? by lazy {
        if (!DevEnvironment.localBridgeEnabled) return@lazy null
        LocalDevBridgeClient().also { bridge ->
            bridge.start(applicationScope) { path, data ->
                when (path) {
                    DataLayerContract.PATH_NAVIGATION_EVENT -> incomingEventHandler.onNavigationEventPayload(data)
                    DataLayerContract.PATH_ACTIVE_ROUTE ->
                        LocalDevBridgeClient.decodeActiveRoute(data)?.let(incomingEventHandler::onActiveRoute)
                }
            }
        }
    }

    val hapticSettings: HapticSettingsRepository by lazy { HapticSettingsRepository(appContext) }

    val vibrationManager: VibrationManager by lazy {
        VibrationManager(appContext, intensity = { hapticSettings.intensityFactor })
    }

    val batteryMonitor: BatteryMonitor by lazy { BatteryMonitor(appContext) }
}
