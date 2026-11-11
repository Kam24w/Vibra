package com.vibra.navigation.tile

import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.vibra.navigation.R
import com.vibra.navigation.VibraApplication

/**
 * Tile de Vibra: desde la esfera, un deslizamiento muestra la ruta activa (destino y
 * distancia) o invita a iniciar una, con un botón a la guía. El contenido sale del
 * mismo repositorio que la app; se refresca al cambiar la ruta (ver [TileRefresher]).
 */
class VibraTileService : TileService() {

    private val container by lazy { (application as VibraApplication).container }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val repository = container.navigationRepository
        // Pide al repositorio comprobar el enlace: el próximo refresco ya lo reflejará.
        repository.requestConnectionRefresh()
        val content = tileContentFor(
            route = repository.activeRoute.value,
            connection = repository.connectionStatus.value,
            paused = repository.guidancePaused.value,
        )
        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(FRESHNESS_MS)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(TileLayout.build(this, content)))
            .build()
        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> = Futures.immediateFuture(
        ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .addIdToImageMapping(
                TileLayout.ICON_ID,
                ResourceBuilders.ImageResource.Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId.Builder().setResourceId(R.drawable.ic_vibration).build(),
                    )
                    .build(),
            )
            .build(),
    )

    private companion object {
        const val RESOURCES_VERSION = "1"
        /** Sin ruta no hace falta refrescar seguido; con ruta lo empuja [TileRefresher]. */
        const val FRESHNESS_MS = 15 * 60 * 1_000L
    }
}
