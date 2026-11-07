package com.vibra.navigation.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * Paleta de marca extraída de design/logo.png y design/diseño.png (ver res/values/colors.xml).
 *
 * En el reloj se usa fondo negro puro (ahorra batería en OLED y es la guía de Wear OS);
 * los paneles van en azul marino y el acento en naranja. Contrastes WCAG:
 *  - Naranja sobre negro ≈ 7.5:1
 *  - Crema sobre panel marino ≈ 9.7:1
 *  - Marino sobre naranja (texto de botones) ≈ 5.6:1
 */
object VibraColors {
    val Navy = Color(0xFF0C2448)
    val NavyPanel = Color(0xFF1F3D57)
    val Orange = Color(0xFFF07A1A)
    val Cream = Color(0xFFF2EFE1)
    val Blue = Color(0xFF2484B4)

    val Background = Color(0xFF000000)
    val Muted = Color(0xFFC9C6B8)
    val Success = Color(0xFF5BD08A)
    val Error = Color(0xFFFF6B5E)
}

private val VibraColorScheme = ColorScheme(
    primary = VibraColors.Orange,
    onPrimary = VibraColors.Navy,
    primaryContainer = VibraColors.NavyPanel,
    onPrimaryContainer = VibraColors.Cream,
    secondary = VibraColors.Blue,
    onSecondary = VibraColors.Cream,
    surfaceContainer = VibraColors.NavyPanel,
    surfaceContainerLow = VibraColors.NavyPanel,
    surfaceContainerHigh = VibraColors.NavyPanel,
    onSurface = VibraColors.Cream,
    onSurfaceVariant = VibraColors.Muted,
    background = VibraColors.Background,
    onBackground = VibraColors.Cream,
    error = VibraColors.Error,
    onError = VibraColors.Navy,
)

@Composable
fun VibraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VibraColorScheme,
        content = content,
    )
}
