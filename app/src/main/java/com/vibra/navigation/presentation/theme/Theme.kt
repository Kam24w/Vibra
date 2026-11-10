package com.vibra.navigation.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * Paleta de marca de Vibra, sacada del logo: azul marino (trazos del reloj y la
 * palabra "Vibra"), azul (ondas izquierdas) y naranja (ondas derechas), sobre un
 * fondo marfil pastel constante en todas las pantallas.
 *
 * Contrastes aproximados sobre el marfil (#F4F1EA):
 *  - Texto principal (marino)         ≈ 11:1
 *  - Texto secundario                 ≈ 5:1
 *  - Naranja de texto (OrangeText)    ≈ 4.6:1
 *  - Marino sobre naranja (botones)   ≈ 4.9:1
 */
object VibraColors {
    /** Marfil pastel: fondo único de toda la app. */
    val Ivory = Color(0xFFF4F1EA)
    val Background = Ivory

    /** Tarjetas: blanco cálido que se separa del marfil. */
    val Surface = Color(0xFFFFFDF8)
    /** Chips, pistas y botones secundarios. */
    val SurfaceHigh = Color(0xFFE8E1D3)
    val Outline = Color(0xFFD5CBB8)

    /** Azul marino para textos e iconos. */
    val NavyInk = Color(0xFF1B365D)
    val OnSurface = NavyInk
    val OnSurfaceMuted = Color(0xFF5B6B80)
    /** Elementos inactivos (pistas, iconos apagados). */
    val Disabled = Color(0xFFA9B2BE)

    /** Azul de las ondas izquierdas del logo. */
    val Blue = Color(0xFF2479C4)

    /** Naranja de las ondas derechas del logo (rellenos y ondas). */
    val Orange = Color(0xFFF7931E)
    /** Naranja oscurecido para textos sobre marfil. */
    val OrangeText = Color(0xFFB45309)
    val OrangeSoft = Color(0xFFFFD9A8)
    val OnOrange = NavyInk

    /** Crema del material de marca: insignia del logo. */
    val Cream = Color(0xFFF4EEE2)

    /** Estado correcto (sincronizado, conectado). */
    val Emerald = Color(0xFF0E9467)

    /** Rojo de alerta accesible para rellenos con texto blanco. */
    val Danger = Color(0xFFC62828)
    /** Rojo para texto o puntos sobre marfil. */
    val DangerText = Color(0xFFC62828)
    val OnDanger = Color.White

    /** Avisos (ALERT): ámbar oscuro para leerse sobre marfil. */
    val Warning = Color(0xFFB7791F)
    val OnWarning = Color.White
}

object VibraGradients {
    /** Firma de marca: azul → naranja, como las ondas del logo. */
    val Brand = Brush.horizontalGradient(listOf(VibraColors.Blue, VibraColors.Orange))

    /** Anillo de marca (para arcos circulares). */
    val BrandSweep = Brush.sweepGradient(
        listOf(VibraColors.Orange, VibraColors.Blue, VibraColors.Blue, VibraColors.Orange),
    )

    /** Tarjeta destacada (ruta activa): naranja de marca. */
    val Highlight = Brush.verticalGradient(listOf(Color(0xFFFFAB45), VibraColors.Orange))

    /** Tarjeta normal: blanco cálido. */
    val Card = Brush.verticalGradient(listOf(VibraColors.Surface, Color(0xFFFBF8F1)))
}

private val VibraColorScheme = ColorScheme(
    primary = VibraColors.Orange,
    primaryDim = VibraColors.Orange,
    onPrimary = VibraColors.OnOrange,
    primaryContainer = VibraColors.SurfaceHigh,
    onPrimaryContainer = VibraColors.OnSurface,
    secondary = VibraColors.Blue,
    secondaryDim = VibraColors.Blue,
    onSecondary = Color.White,
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
