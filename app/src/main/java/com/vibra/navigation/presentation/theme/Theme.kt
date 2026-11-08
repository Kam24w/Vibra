package com.vibra.navigation.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * Paleta OLED de Vibra, coherente con WanderAI (acento naranja, esmeralda para
 * estados correctos y grises pizarra neutros).
 *
 * El fondo es negro puro: en AMOLED los píxeles negros van apagados (ahorro de
 * batería) y el contraste máximo mejora la lectura a pleno sol. Contrastes:
 *  - Texto principal sobre negro        ≈ 19:1
 *  - Texto secundario sobre negro       ≈ 7.6:1  (sobre Surface ≈ 6.9:1)
 *  - Negro sobre naranja                ≈ 7.2:1
 *  - Blanco sobre Danger                ≈ 4.8:1  (AA)
 *  - Naranja / esmeralda / ámbar sobre negro > 7:1
 */
object VibraColors {
    val Background = Color.Black
    /** Tarjetas: pizarra muy oscuro, se distingue del negro sin encender mucho el panel. */
    val Surface = Color(0xFF0F172A)
    val SurfaceHigh = Color(0xFF1E293B)
    val Outline = Color(0xFF334155)

    val OnSurface = Color(0xFFF8FAFC)
    val OnSurfaceMuted = Color(0xFF94A3B8)
    /** Elementos inactivos (pistas, iconos apagados). */
    val Disabled = Color(0xFF475569)

    val Orange = Color(0xFFFF6D00)
    val OrangeSoft = Color(0xFFFFB27A)
    val OnOrange = Color.Black

    val Emerald = Color(0xFF10B981)
    val EmeraldSoft = Color(0xFF6EE7B7)

    /** Rojo de alerta accesible para rellenos con texto blanco. */
    val Danger = Color(0xFFDC2626)
    /** Rojo para texto o puntos sobre negro. */
    val DangerText = Color(0xFFF87171)
    val OnDanger = Color.White

    /** Avisos (ALERT). */
    val Warning = Color(0xFFFBBF24)
    val OnWarning = Color.Black
}

object VibraGradients {
    /** Tarjeta destacada (ruta activa): naranja que se intensifica hacia abajo. */
    val Highlight = Brush.verticalGradient(listOf(Color(0xFFFF8A33), VibraColors.Orange))

    /** Tarjeta de maniobra: pizarra con algo de profundidad. */
    val Card = Brush.verticalGradient(listOf(VibraColors.SurfaceHigh, VibraColors.Surface))
}

private val VibraColorScheme = ColorScheme(
    primary = VibraColors.Orange,
    primaryDim = VibraColors.Orange,
    onPrimary = VibraColors.OnOrange,
    primaryContainer = VibraColors.SurfaceHigh,
    onPrimaryContainer = VibraColors.OnSurface,
    secondary = VibraColors.Emerald,
    secondaryDim = VibraColors.Emerald,
    onSecondary = Color.Black,
    secondaryContainer = VibraColors.Surface,
    onSecondaryContainer = VibraColors.OnSurface,
    tertiary = VibraColors.Warning,
    onTertiary = VibraColors.OnWarning,
    surfaceContainerLow = VibraColors.Surface,
    surfaceContainer = VibraColors.Surface,
    surfaceContainerHigh = VibraColors.SurfaceHigh,
    onSurface = VibraColors.OnSurface,
    onSurfaceVariant = VibraColors.OnSurfaceMuted,
    outline = VibraColors.Outline,
    outlineVariant = VibraColors.Outline,
    background = VibraColors.Background,
    onBackground = VibraColors.OnSurface,
    error = VibraColors.Danger,
    onError = VibraColors.OnDanger,
)

@Composable
fun VibraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VibraColorScheme,
        content = content,
    )
}
