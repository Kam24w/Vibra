package com.vibra.navigation.ongoing

import com.vibra.navigation.data.repository.NavigationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Mantiene la actividad en curso sincronizada con la ruta: aparece cuando WanderAI
 * activa una ruta, se actualiza con la distancia/pausa y desaparece al terminar.
 * Vive en el ámbito del proceso, así funciona aunque la UI esté cerrada.
 */
class OngoingGuidanceController(
    private val repository: NavigationRepository,
    private val show: (OngoingGuidance) -> Unit,
    private val cancel: () -> Unit,
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job != null) return
        job = scope.launch {
            combine(repository.activeRoute, repository.latestEvent, repository.guidancePaused, ::ongoingGuidanceFor)
                .distinctUntilChanged()
                .collect { guidance -> if (guidance != null) show(guidance) else cancel() }
        }
    }
}
