package com.vibra.navigation.presentation.ambient

import androidx.compose.runtime.compositionLocalOf

/**
 * True mientras el reloj está en modo ambiente (pantalla atenuada, muñeca abajo).
 * La UI debe pasar a un diseño de bajo consumo: fondo negro, poco brillo y sin animar.
 */
val LocalAmbientMode = compositionLocalOf { false }
