package com.vibra.navigation.service

import android.util.Log
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.vibra.navigation.VibraApplication
import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.parser.ParseResult
import com.vibra.navigation.data.repository.EventDisposition
import com.vibra.navigation.data.repository.toActiveRoute
import com.vibra.navigation.data.wear.DataLayerContract

/**
 * Punto de entrada de la Data Layer, funcione la UI o no.
 *
 * Batería: el sistema solo enlaza (y despierta) este servicio cuando llega un
 * evento que coincide con los `<intent-filter>` del manifest (filtrados por
 * path), y lo destruye al terminar. No hay polling, wakelocks propios ni
 * servicio en primer plano. Todo el trabajo es síncrono y corto.
 */
class VibraListenerService : WearableListenerService() {

    private val container by lazy { (application as VibraApplication).container }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != DataLayerContract.PATH_NAVIGATION_EVENT) return

        when (val result = container.parser.parse(messageEvent.data)) {
            is ParseResult.Failure -> Log.w(TAG, "Evento descartado: ${result.reason}", result.cause)
            is ParseResult.Success -> {
                val event = result.event
                val repository = container.navigationRepository
                when (repository.onNavigationEvent(event)) {
                    EventDisposition.NEW ->
                        if (repository.guidancePaused.value) {
                            Log.i(TAG, "Guía en pausa: ${event.action} sin vibración")
                        } else {
                            container.vibrationManager.vibrate(event)
                        }
                    EventDisposition.STALE -> Log.i(TAG, "Evento atrasado, se muestra sin vibrar: ${event.action}")
                    EventDisposition.DUPLICATE -> Log.d(TAG, "Evento duplicado ignorado: ${event.timestamp}")
                }
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // El buffer se libera al volver de este método: leer todo de forma síncrona.
        dataEvents.forEach { event ->
            if (event.dataItem.uri.path != DataLayerContract.PATH_ACTIVE_ROUTE) return@forEach
            val route = when (event.type) {
                DataEvent.TYPE_CHANGED -> runCatching {
                    DataMapItem.fromDataItem(event.dataItem).dataMap.toActiveRoute()
                }.getOrElse {
                    Log.w(TAG, "DataItem de ruta mal formado", it)
                    return@forEach
                }
                DataEvent.TYPE_DELETED -> ActiveRoute.Inactive
                else -> return@forEach
            }
            val wasActive = container.navigationRepository.activeRoute.value.routeActive
            container.navigationRepository.onActiveRouteChanged(route)
            if (wasActive && !route.routeActive) container.vibrationManager.cancel()
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        container.navigationRepository.requestConnectionRefresh()
    }

    private companion object {
        const val TAG = "VibraListenerService"
    }
}
