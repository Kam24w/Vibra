package com.vibra.navigation.data.parser

import com.vibra.navigation.data.model.NavigationEvent
import com.vibra.navigation.data.model.UserAction
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

sealed interface ParseResult {
    data class Success(val event: NavigationEvent) : ParseResult
    data class Failure(val reason: String, val cause: Throwable? = null) : ParseResult
}

/** Parser puro (sin dependencias de Android) para poder probarlo en JVM. */
class NavigationEventParser(
    private val json: Json = DefaultJson,
) {

    fun parse(payload: ByteArray): ParseResult {
        if (payload.isEmpty()) return ParseResult.Failure("Payload vacío")
        if (payload.size > MAX_PAYLOAD_BYTES) return ParseResult.Failure("Payload demasiado grande (${payload.size} B)")

        return try {
            val event = json.decodeFromString(NavigationEvent.serializer(), payload.decodeToString())
            validate(event)
        } catch (e: SerializationException) {
            ParseResult.Failure("JSON inválido: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            ParseResult.Failure("JSON inválido: ${e.message}", e)
        }
    }

    fun encode(action: UserAction): ByteArray =
        json.encodeToString(UserAction.serializer(), action).encodeToByteArray()

    private fun validate(event: NavigationEvent): ParseResult {
        if (event.timestamp <= 0L) return ParseResult.Failure("timestamp ausente o inválido")
        if (event.distanceMeters != null && event.distanceMeters < 0) {
            return ParseResult.Failure("distance_meters negativo: ${event.distanceMeters}")
        }
        return ParseResult.Success(event)
    }

    companion object {
        /** Los mensajes de MessageClient tienen límite práctico de ~100 KB; un evento es << 1 KB. */
        private const val MAX_PAYLOAD_BYTES = 16 * 1024

        val DefaultJson = Json {
            ignoreUnknownKeys = true   // tolera campos nuevos del teléfono
            coerceInputValues = true   // enums desconocidos / nulls -> valor por defecto
            explicitNulls = false
            isLenient = false
        }
    }
}
