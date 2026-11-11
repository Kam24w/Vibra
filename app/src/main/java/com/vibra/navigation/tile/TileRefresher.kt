package com.vibra.navigation.tile

import com.vibra.navigation.data.repository.NavigationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Pide al sistema redibujar el Tile solo cuando cambia lo que muestra (ruta, distancia,
 * pausa o enlace), no con cada evento: así el Tile está al día sin gastar batería.
 */
class TileRefresher(
    private val repository: NavigationRepository,
    private val requestUpdate: () -> Unit,
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job != null) return
        job = scope.launch {
            combine(repository.activeRoute, repository.connectionStatus, repository.guidancePaused, ::tileContentFor)
                .distinctUntilChanged()
                // El primer valor es el estado actual: el sistema ya lo pide al mostrar el Tile.
                .drop(1)
                .collect { requestUpdate() }
        }
    }
}
