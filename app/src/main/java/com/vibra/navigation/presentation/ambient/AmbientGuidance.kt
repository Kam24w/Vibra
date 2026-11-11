package com.vibra.navigation.presentation.ambient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.timeTextCurvedText
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.vibra.navigation.R
import com.vibra.navigation.data.model.ActiveRoute
import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.presentation.components.formatDistance
import com.vibra.navigation.presentation.guidance.GuidanceUiState
import com.vibra.navigation.presentation.guidance.ManeuverIndicator
import com.vibra.navigation.presentation.guidance.ManeuverUi

/**
 * Guía en modo ambiente: fondo negro (los píxeles apagados no gastan en OLED ni se
 * marcan), solo trazos grises/blancos y sin animaciones. Muestra lo mínimo para
 * seguir caminando: la maniobra, la distancia y el destino.
 */
@Composable
fun AmbientGuidance(state: GuidanceUiState) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // La hora sigue visible en ambiente, en gris y sin fondo.
        TimeText(backgroundColor = Color.Transparent, modifier = Modifier.align(Alignment.TopCenter)) { time ->
            timeTextCurvedText(time, CurvedTextStyle(color = AMBIENT_TEXT))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
            val maneuver = state.maneuver
            if (maneuver != null) {
                ManeuverIndicator(action = maneuver.action, eventId = maneuver.id, color = AMBIENT_TEXT, size = 72.dp)
                maneuver.distanceMeters?.takeIf { it > 0 }?.let {
                    Text(formatDistance(it.toDouble()), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = state.route.currentDestination?.takeIf { state.route.routeActive }
                    ?: stringResource(R.string.home_no_route),
                color = AMBIENT_TEXT,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.isPaused) {
                Text(stringResource(R.string.guidance_paused_banner), color = AMBIENT_TEXT, fontSize = 12.sp)
            }
        }
    }
}

/** Gris claro: legible en ambiente sin encender demasiados píxeles a pleno brillo. */
private val AMBIENT_TEXT = Color(0xFFBDBDBD)

@WearPreviewDevices
@Composable
private fun AmbientGuidancePreview() {
    AmbientGuidance(
        GuidanceUiState(
            route = ActiveRoute("La Casita del Cuy", 820.0, routeActive = true),
            maneuver = ManeuverUi(1L, NavigationAction.TURN_LEFT, "Gira a la izquierda", 40),
        ),
    )
}
