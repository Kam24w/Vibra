package com.vibra.navigation.data.wear

import android.os.Build
import android.util.Log
import com.vibra.navigation.BuildConfig
import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.repository.parseDistanceMeters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/** Detección del entorno de desarrollo local (emulador). */
object DevEnvironment {
    val isEmulator: Boolean by lazy {
        Build.FINGERPRINT.startsWith("generic") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu")
    }

    /** Solo debug + emulador. En un reloj físico (o en release) siempre es false. */
    val localBridgeEnabled: Boolean get() = BuildConfig.DEBUG && isEmulator
}

/**
 * Cliente del puente de desarrollo de WanderAI (`LocalDevBridge` en el teléfono), para
 * cuando Google Play Services del emulador no empareja ("Got 0 accounts",
 * consentimiento global pendiente…).
 *
 * Se conecta a `10.0.2.2:[PORT]` (el host visto desde el emulador), que llega al
 * teléfono (físico o emulado) con `adb -s <serial-del-TELEFONO> forward tcp:5650 tcp:5650`.
 * Ojo: el serial es el del teléfono, no el del reloj.
 * Los frames llevan los mismos paths y payloads que [DataLayerContract].
 *
 * Nunca se instancia en un reloj físico: allí solo se usa la Data Layer.
 */
class LocalDevBridgeClient(
    private val host: String = HOST,
    private val port: Int = PORT,
) {
    fun interface FrameHandler {
        fun onFrame(path: String, data: ByteArray)
    }

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val writeMutex = Mutex()
    @Volatile private var socket: Socket? = null
    @Volatile private var output: DataOutputStream? = null
    private var job: Job? = null

    fun start(scope: CoroutineScope, handler: FrameHandler) {
        if (job != null) return
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                runCatching { connectAndRead(handler) }
                    .onFailure { Log.d(TAG, "Puente local no disponible: ${it.message}") }
                delay(RECONNECT_DELAY_MS)
            }
        }
    }

    private suspend fun connectAndRead(handler: FrameHandler) = coroutineScope {
        val s = Socket()
        var pinger: Job? = null
        try {
            s.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            s.tcpNoDelay = true
            // Latido: sin datos (ni PING) del teléfono en READ_TIMEOUT_MS, el enlace está muerto.
            s.soTimeout = READ_TIMEOUT_MS
            writeMutex.withLock {
                socket = s
                output = DataOutputStream(BufferedOutputStream(s.getOutputStream()))
            }
            _connected.value = true
            Log.i(TAG, "Conectado a WanderAI por el puente local $host:$port")
            pinger = launch(Dispatchers.IO) {
                while (isActive) {
                    delay(PING_INTERVAL_MS)
                    if (!send(PATH_PING, ByteArray(0))) break
                }
            }
            val input = DataInputStream(BufferedInputStream(s.getInputStream()))
            while (true) {
                val path = input.readUTF()
                val size = input.readInt()
                if (size !in 0..MAX_FRAME_BYTES) throw IOException("Frame inválido: $size bytes")
                val data = ByteArray(size).also { input.readFully(it) }
                if (path == PATH_PING) continue
                runCatching { handler.onFrame(path, data) }
                    .onFailure { Log.w(TAG, "Error procesando frame $path", it) }
            }
        } finally {
            pinger?.cancel()
            writeMutex.withLock {
                if (socket === s) {
                    socket = null
                    output = null
                }
            }
            runCatching { s.close() }
            if (_connected.value) Log.w(TAG, "Puente local desconectado")
            _connected.value = false
        }
    }

    /** false si no hay conexión o la escritura falla. */
    suspend fun send(path: String, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val out = output ?: return@withLock false
            runCatching {
                out.writeUTF(path)
                out.writeInt(data.size)
                out.write(data)
                out.flush()
            }.onFailure {
                Log.w(TAG, "Escritura en el puente fallida", it)
                runCatching { socket?.close() }
            }.isSuccess
        }
    }

    companion object {
        const val HOST = "10.0.2.2"
        const val PORT = 5650
        /** Latido en ambos sentidos (igual que `LocalDevBridge.PATH_PING` del teléfono). */
        const val PATH_PING = "/bridge/ping"
        private const val PING_INTERVAL_MS = 3_000L
        private const val READ_TIMEOUT_MS = 10_000
        const val NODE_NAME = "WanderAI (puente ADB)"
        private const val TAG = "VibraDevBridge"
        // Windows tarda ~2 s en rechazar un puerto cerrado en loopback: con 2 s el
        // "connection refused" se disfrazaba de timeout.
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val RECONNECT_DELAY_MS = 2_000L
        private const val MAX_FRAME_BYTES = 64 * 1024
        private const val FIELD_SEPARATOR = '\u001F'

        /** Inverso de `LocalDevBridge.encodeActiveRoute` del teléfono: `activa␟destino␟distancia`. */
        fun decodeActiveRoute(data: ByteArray): ActiveRoute? {
            val parts = data.toString(Charsets.UTF_8).split(FIELD_SEPARATOR, limit = 3)
            if (parts.size != 3) return null
            return ActiveRoute(
                currentDestination = parts[1].takeIf { it.isNotBlank() },
                remainingDistanceMeters = parseDistanceMeters(parts[2]),
                routeActive = parts[0].toBooleanStrictOrNull() ?: return null,
            )
        }
    }
}
