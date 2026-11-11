package com.vibra.navigation.tile

import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.ConnectionStatus

/** Qué abre el botón principal del Tile. */
enum class TileAction { OPEN_GUIDANCE, OPEN_APP }

/** Estado que pinta el Tile; puro (sin Android) para poder testearlo. */
sealed interface TileContent {
    val action: TileAction

    /** Hay ruta: destino, distancia restante (m) y si la guía está en pausa. */
    data class Route(val destination: String?, val remainingMeters: Double?, val paused: Boolean) : TileContent {
        override val action = TileAction.OPEN_GUIDANCE
    }

    /** Sin ruta: invita a iniciar una en WanderAI e indica si el celular está enlazado. */
    data class Idle(val phoneLinked: Boolean) : TileContent {
        override val action = TileAction.OPEN_APP
    }
}

fun tileContentFor(route: ActiveRoute, connection: ConnectionStatus, paused: Boolean): TileContent =
    if (route.routeActive) {
        TileContent.Route(route.currentDestination, route.remainingDistanceMeters, paused)
    } else {
        TileContent.Idle(phoneLinked = connection is ConnectionStatus.Connected)
    }
