package com.vibra.navigation.data.repository

import android.net.Uri
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataRequest
import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.ConnectionStatus
import com.vibra.navigation.data.model.NavigationEvent
import com.vibra.navigation.data.model.UserAction
import com.vibra.navigation.data.model.UserActionType
import com.vibra.navigation.data.parser.NavigationEventParser
import com.vibra.navigation.data.wear.DataLayerContract
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Implementación sobre la Wear OS Data Layer API.
 *
 * Es un singleton de proceso (ver [com.vibra.navigation.di.AppContainer]): el
 * WearableListenerService escribe en él y el ViewModel lo observa, así UI y
 * servicio comparten una única fuente de verdad sin listeners duplicados.
 */
class WearNavigationRepository(
    private val dataClient: DataClient,
    private val messageClient: MessageClient,
    private val nodeClient: NodeClient,
    private val capabilityClient: CapabilityClient,
    private val parser: NavigationEventParser,
    private val clock: () -> Long = System::currentTimeMillis,
) : NavigationRepository {

    private val _activeRoute = MutableStateFlow(ActiveRoute.Inactive)
    override val activeRoute: StateFlow<ActiveRoute> = _activeRoute.asStateFlow()

    private val _latestEvent = MutableStateFlow<NavigationEvent?>(null)
    override val latestEvent: StateFlow<NavigationEvent?> = _latestEvent.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Unknown)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    override suspend fun refresh() {
        loadActiveRoute()
        resolveTargetNode()
    }

    override suspend fun sendUserAction(action: UserActionType): Result<Unit> = runCatchingWear {
        val node = resolveTargetNode() ?: error("WanderAI no está conectado")
        val payload = parser.encode(UserAction(action, clock()))
        withTimeout(SEND_TIMEOUT_MS) {
            messageClient.sendMessage(node.id, DataLayerContract.PATH_USER_ACTION, payload).await()
        }
        Unit
    }

    override fun onNavigationEvent(event: NavigationEvent): EventDisposition {
        var disposition = EventDisposition.NEW
        _latestEvent.update { previous ->
            when {
                previous != null && event.timestamp <= previous.timestamp -> {
                    disposition = EventDisposition.DUPLICATE
                    previous
                }
                clock() - event.timestamp > STALE_EVENT_MS -> {
                    disposition = EventDisposition.STALE
                    event
                }
                else -> {
                    disposition = EventDisposition.NEW
                    event
                }
            }
        }
        // Si recibimos un mensaje, el teléfono está alcanzable.
        if (_connectionStatus.value !is ConnectionStatus.Connected) {
            _connectionStatus.value = ConnectionStatus.Connected(nodeName = "WanderAI")
        }
        return disposition
    }

    override fun onActiveRouteChanged(route: ActiveRoute) {
        _activeRoute.value = route
        if (!route.routeActive) _latestEvent.value = null
    }

    override fun onConnectionChanged(status: ConnectionStatus) {
        _connectionStatus.value = status
    }

    private suspend fun loadActiveRoute() {
        runCatchingWear {
            // wear://*/<path>: el DataItem de cualquier nodo emparejado.
            val uri = Uri.Builder()
                .scheme(PutDataRequest.WEAR_URI_SCHEME)
                .path(DataLayerContract.PATH_ACTIVE_ROUTE)
                .build()
            val buffer = dataClient.getDataItems(uri).await()
            try {
                buffer.firstOrNull()?.let { item ->
                    _activeRoute.value = DataMapItem.fromDataItem(item).dataMap.toActiveRoute()
                }
            } finally {
                buffer.release()
            }
        }.onFailure { Log.w(TAG, "No se pudo leer $PATH", it) }
    }

    /** Prefiere el nodo con la capability de WanderAI; si no existe, cualquier nodo conectado. */
    private suspend fun resolveTargetNode(): Node? {
        val result = runCatchingWear {
            val capable = capabilityClient
                .getCapability(DataLayerContract.CAPABILITY_WANDERAI, CapabilityClient.FILTER_REACHABLE)
                .await()
                .nodes
            val candidates = capable.ifEmpty { nodeClient.connectedNodes.await().toSet() }
            candidates.firstOrNull { it.isNearby } ?: candidates.firstOrNull()
        }

        _connectionStatus.value = result.fold(
            onSuccess = { node -> node?.let { ConnectionStatus.Connected(it.displayName) } ?: ConnectionStatus.Disconnected },
            onFailure = { e ->
                if (e is ApiException && e.statusCode == CommonStatusCodes.API_NOT_CONNECTED) {
                    ConnectionStatus.Unavailable("Wearable API no disponible")
                } else {
                    ConnectionStatus.Disconnected
                }
            },
        )
        return result.getOrNull()
    }

    /** Como runCatching pero sin tragarse la cancelación de corrutinas. */
    private inline fun <T> runCatchingWear(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error en Data Layer", e)
            Result.failure(e)
        }

    companion object {
        private const val TAG = "WearNavigationRepo"
        private const val PATH = DataLayerContract.PATH_ACTIVE_ROUTE
        private const val SEND_TIMEOUT_MS = 5_000L

        /** Eventos con más antigüedad que esto se muestran pero no vibran. */
        const val STALE_EVENT_MS = 30_000L
    }
}

/**
 * Lectura defensiva del DataMap: el teléfono podría escribir la distancia como
 * Int, Long, Float o Double según la implementación.
 */
fun DataMap.toActiveRoute(): ActiveRoute = ActiveRoute(
    currentDestination = getString(DataLayerContract.KEY_CURRENT_DESTINATION),
    remainingDistanceMeters = (get<Any?>(DataLayerContract.KEY_REMAINING_DISTANCE) as? Number)?.toDouble(),
    routeActive = getBoolean(DataLayerContract.KEY_ROUTE_ACTIVE, false),
)
