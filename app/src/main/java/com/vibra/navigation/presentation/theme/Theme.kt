package com.vibra.navigation.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * Paleta oficial de Vibra (mockup design/diseño.png).
 *
 * Fondos crema con degradado y componentes en azul marino profundo. El naranja se usa
 * como relleno de acciones principales (con texto marino) o sobre marino, nunca como
 * texto sobre crema, para conservar el contraste en AMOLED a pleno sol:
 *  - Marino sobre crema        ≈ 13:1
 *  - Marino sobre naranja      ≈ 5.4:1
 *  - Naranja sobre marino      ≈ 5.4:1
 *  - Crema sobre marino        ≈ 13:1
 */
object VibraColors {
    val Navy = Color(0xFF0F2537)
    val NavyLight = Color(0xFF1D3D57)
    val Orange = Color(0xFFFF6B00)
    val OrangeSoft = Color(0xFFFFB27A)

    val CreamLight = Color(0xFFFBF8F1)
    val Cream = Color(0xFFF4EEE2)
    val Beige = Color(0xFFE6DBC5)

    /** Texto secundario sobre crema (≈ 6:1). */
    val NavyMuted = Color(0xFF4A5B6B)
    /** Texto secundario sobre marino. */
    val CreamMuted = Color(0xFFC9D2DA)

    val Success = Color(0xFF1E7D46)
    val Error = Color(0xFFC62828)
}

object VibraGradients {
    /** Fondo de pantalla: crema luminoso en el centro que se oscurece hacia el bisel. */
    val Screen = Brush.radialGradient(
        0f to VibraColors.CreamLight,
        0.6f to VibraColors.Cream,
        1f to VibraColors.Beige,
    )

    /** Tarjetas marinas con algo de profundidad. */
    val NavyCard = Brush.verticalGradient(listOf(VibraColors.NavyLight, VibraColors.Navy))

    /** Splash: halo cálido sobre crema. */
    val Splash = Brush.radialGradient(
        0f to Color(0xFFFFF1E3),
        0.55f to VibraColors.Cream,
        1f to VibraColors.Beige,
    )
}

private val VibraColorScheme = ColorScheme(
    primary = VibraColors.Orange,
    primaryDim = VibraColors.Orange,
    onPrimary = VibraColors.Navy,
    primaryContainer = VibraColors.Navy,
    onPrimaryContainer = VibraColors.Cream,
    secondary = VibraColors.Navy,
    secondaryDim = VibraColors.Navy,
    onSecondary = VibraColors.Cream,
    secondaryContainer = VibraColors.Navy,
    onSecondaryContainer = VibraColors.Cream,
    tertiary = VibraColors.Orange,
    onTertiary = VibraColors.Navy,
    surfaceContainerLow = VibraColors.Beige,
    surfaceContainer = VibraColors.Beige,
    surfaceContainerHigh = VibraColors.Beige,
    onSurface = VibraColors.Navy,
    onSurfaceVariant = VibraColors.NavyMuted,
    outline = VibraColors.NavyMuted,
    outlineVariant = VibraColors.Beige,
    background = VibraColors.Cream,
    onBackground = VibraColors.Navy,
    error = VibraColors.Error,
    onError = VibraColors.CreamLight,
)

@Composable
fun VibraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VibraColorScheme,
        content = content,
    )
}
