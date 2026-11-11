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
import com.vibra.navigation.data.wear.LocalDevBridgeClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Implementación sobre la Wear OS Data Layer API, pensada para sobrevivir a
 * desconexiones (Bluetooth que se cae, emulador emparejado por ADB que se reinicia…):
 *
 *  - **Vigilancia activa solo con UI visible:** mientras alguien observa
 *    [connectionStatus] se escucha el CapabilityClient y se comprueba la conexión
 *    cada [POLL_INTERVAL_MS]. Sin observadores no hay trabajo en segundo plano.
 *  - **Resincronización al reconectar:** al pasar a conectado se relee el DataItem
 *    de la ruta (pudo cambiar sin enlace) y se reenvía la acción pendiente.
 *  - **Envíos con reintentos** y backoff exponencial, resolviendo el nodo en cada intento.
 *
 * Es un singleton de proceso (ver [com.vibra.navigation.di.AppContainer]).
 *
 * [devBridge] solo existe en emuladores (debug): si Play services no devuelve nodos o
 * falla ("Got 0 accounts", consentimiento global…), se usa el puente por túnel ADB.
 * En un reloj físico es null y el comportamiento es exactamente el de la Data Layer.
 */
class WearNavigationRepository(
    private val dataClient: DataClient,
    private val messageClient: MessageClient,
    private val nodeClient: NodeClient,
    private val capabilityClient: CapabilityClient,
    private val parser: NavigationEventParser,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val devBridge: LocalDevBridgeClient? = null,
) : NavigationRepository {

    private val _activeRoute = MutableStateFlow(ActiveRoute.Inactive)
    override val activeRoute: StateFlow<ActiveRoute> = _activeRoute.asStateFlow()

    private val _latestEvent = MutableStateFlow<NavigationEvent?>(null)
    override val latestEvent: StateFlow<NavigationEvent?> = _latestEvent.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Unknown)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _lastSyncAt = MutableStateFlow<Long?>(null)
    override val lastSyncAt: StateFlow<Long?> = _lastSyncAt.asStateFlow()

    private val _guidancePaused = MutableStateFlow(false)
    override val guidancePaused: StateFlow<Boolean> = _guidancePaused.asStateFlow()

    /**
     * True mientras el enlace es el puente ADB de depuración. Entonces la ruta llega por
     * el puente (que la reenvía al conectar) y NO se lee la Data Layer: en un emulador
     * puede guardar un DataItem viejo de otro emparejamiento que pisaría la ruta real.
     */
    @Volatile private var viaBridge = false

    /** Última acción con estado que no se pudo entregar. */
    @Volatile private var pendingAction: UserActionType? = null

    /** Serializa los envíos para que no se adelanten entre sí (p. ej. pausa/reanudar). */
    private val sendMutex = Mutex()

    private val capabilityListener = CapabilityClient.OnCapabilityChangedListener {
        // No se usa info.nodes directamente: si WanderAI no declara la capability la
        // lista vendría vacía aunque el teléfono esté conectado. Se re-resuelve.
        requestConnectionRefresh()
    }

    init {
        devBridge?.let { bridge ->
            scope.launch { bridge.connected.collect { requestConnectionRefresh() } }
        }
        scope.launch {
            _connectionStatus.subscriptionCount
                .map { it > 0 }
                .distinctUntilChanged()
                .collectLatest { observed -> if (observed) monitorConnection() }
        }
    }

    override suspend fun refresh() {
        resolveTargetNode()
        loadActiveRoute()
    }

    override fun requestConnectionRefresh() {
        scope.launch { resolveTargetNode() }
    }

    override suspend fun sendUserAction(action: UserActionType): Result<Unit> {
        val result = sendMutex.withLock { sendWithRetry(action) }
        if (result.isSuccess) {
            if (pendingAction == action) pendingAction = null
        } else if (action.isStateful()) {
            pendingAction = action
        }
        return result
    }

    override fun setGuidancePaused(paused: Boolean) {
        _guidancePaused.value = paused
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
        markSynced()
        // Si llega un mensaje, el teléfono está alcanzable: confirmar el nodo real.
        if (_connectionStatus.value !is ConnectionStatus.Connected) requestConnectionRefresh()
        return disposition
    }

    override fun onActiveRouteChanged(route: ActiveRoute) {
        _activeRoute.value = route
        if (!route.routeActive) {
            _latestEvent.value = null
            _guidancePaused.value = false
        }
        markSynced()
    }

    // ---------------------------------------------------------------------------------

    private suspend fun monitorConnection() {
        runCatchingWear {
            capabilityClient.addListener(capabilityListener, DataLayerContract.CAPABILITY_WANDERAI).await()
        }
        try {
            while (true) {
                resolveTargetNode()
                delay(POLL_INTERVAL_MS)
            }
        } finally {
            withContext(NonCancellable) {
                runCatchingWear { capabilityClient.removeListener(capabilityListener).await() }
            }
        }
    }

    private suspend fun sendWithRetry(action: UserActionType): Result<Unit> {
        var last: Result<Unit> = Result.failure(IllegalStateException("Sin intentos"))
        for (attempt in 0 until MAX_SEND_ATTEMPTS) {
            last = runCatchingWear {
                val node = resolveTargetNode()
                val bridge = devBridge?.takeIf { it.connected.value }
                if (node == null && bridge == null) throw IllegalStateException("WanderAI no está conectado")
                val payload = parser.encode(UserAction(action, clock()))
                withTimeout(SEND_TIMEOUT_MS) {
                    if (node != null) {
                        messageClient.sendMessage(node.id, DataLayerContract.PATH_USER_ACTION, payload).await()
                    } else {
                        check(bridge!!.send(DataLayerContract.PATH_USER_ACTION, payload)) { "Puente local sin conexión" }
                    }
                }
                markSynced()
            }
            if (last.isSuccess) return last
            if (attempt < MAX_SEND_ATTEMPTS - 1) delay(RETRY_BASE_DELAY_MS shl attempt)
        }
        return last
    }

    private suspend fun loadActiveRoute() {
        if (viaBridge) return
        runCatchingWear {
            // Sin host en la URI: DataItems de cualquier nodo emparejado.
            val uri = Uri.Builder()
                .scheme(PutDataRequest.WEAR_URI_SCHEME)
                .path(DataLayerContract.PATH_ACTIVE_ROUTE)
                .build()
            val buffer = dataClient.getDataItems(uri).await()
            try {
                val item = buffer.firstOrNull()
                if (item != null) {
                    onActiveRouteChanged(DataMapItem.fromDataItem(item).dataMap.toActiveRoute())
                }
            } finally {
                buffer.release()
            }
        }
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

        // Emulador sin emparejamiento de Play services: el puente ADB cuenta como conexión.
        viaBridge = result.getOrNull() == null && devBridge?.connected?.value == true
        if (viaBridge) {
            updateConnection(ConnectionStatus.Connected(LocalDevBridgeClient.NODE_NAME, isNearby = true, viaDevBridge = true))
            return null
        }

        updateConnection(
            result.fold(
                onSuccess = { node ->
                    node?.let { ConnectionStatus.Connected(it.displayName, it.isNearby) }
                        ?: ConnectionStatus.Disconnected
                },
                onFailure = { e ->
                    if (e is ApiException && e.statusCode == CommonStatusCodes.API_NOT_CONNECTED) {
                        ConnectionStatus.Unavailable("Wearable API no disponible")
                    } else {
                        ConnectionStatus.Disconnected
                    }
                },
            ),
        )
        return result.getOrNull()
    }

    private fun updateConnection(status: ConnectionStatus) {
        val previous = _connectionStatus.getAndUpdate { status }
        if (status is ConnectionStatus.Connected && previous !is ConnectionStatus.Connected) {
            Log.i(TAG, "Conectado con ${status.nodeName}: resincronizando")
            scope.launch { onReconnected() }
        } else if (status !is ConnectionStatus.Connected && previous is ConnectionStatus.Connected) {
            Log.w(TAG, "Enlace con WanderAI perdido: $status")
        }
    }

    private suspend fun onReconnected() {
        loadActiveRoute()
        pendingAction?.let { action ->
            Log.i(TAG, "Reenviando acción pendiente $action")
            sendUserAction(action)
        }
    }

    private fun markSynced() {
        _lastSyncAt.value = clock()
    }

    /** Como runCatching pero sin tragarse la cancelación de corrutinas. */
    private inline fun <T> runCatchingWear(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error en Data Layer: ${e.message}")
            Result.failure(e)
        }

    private fun UserActionType.isStateful() =
        this != UserActionType.REPEAT_INSTRUCTION && this != UserActionType.ACKNOWLEDGE

    companion object {
        private const val TAG = "WearNavigationRepo"
        private const val SEND_TIMEOUT_MS = 5_000L
        private const val MAX_SEND_ATTEMPTS = 3
        private const val RETRY_BASE_DELAY_MS = 500L
        const val POLL_INTERVAL_MS = 10_000L

        /** Eventos con más antigüedad que esto se muestran pero no vibran. */
        const val STALE_EVENT_MS = 30_000L
    }
}

/**
 * Lectura defensiva del DataMap: el teléfono podría escribir la distancia como
 * Int, Long, Float, Double o texto formateado (WanderAI envía "850 m" / "1.4 km").
 */
fun DataMap.toActiveRoute(): ActiveRoute = ActiveRoute(
    currentDestination = getString(DataLayerContract.KEY_CURRENT_DESTINATION)?.takeIf { it.isNotBlank() },
    remainingDistanceMeters = when (val raw = get<Any?>(DataLayerContract.KEY_REMAINING_DISTANCE)) {
        is Number -> raw.toDouble()
        is String -> parseDistanceMeters(raw)
        else -> null
    },
    routeActive = getBoolean(DataLayerContract.KEY_ROUTE_ACTIVE, false),
)

private val DISTANCE_TEXT = Regex("""^\s*(\d+(?:[.,]\d+)?)\s*(km|m)?\s*$""", RegexOption.IGNORE_CASE)

/** "850 m" -> 850.0, "1.4 km" / "1,4 km" -> 1400.0, "120" -> 120.0; null si no se entiende. */
internal fun parseDistanceMeters(text: String): Double? {
    val match = DISTANCE_TEXT.matchEntire(text) ?: return null
    val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    return if (match.groupValues[2].equals("km", ignoreCase = true)) value * 1_000 else value
}
