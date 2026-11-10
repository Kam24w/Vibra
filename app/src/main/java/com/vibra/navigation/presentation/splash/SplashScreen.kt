package com.vibra.navigation.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.vibra.navigation.R
import com.vibra.navigation.presentation.components.BrandWaves
import com.vibra.navigation.presentation.components.VibraBackground
import com.vibra.navigation.presentation.components.VibraLogoBadge
import com.vibra.navigation.presentation.theme.VibraColors
import com.vibra.navigation.presentation.theme.VibraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla 2: presentación de marca sobre marfil pastel. Continúa al splash del
 * sistema (pantalla 1, mismo logo sobre marfil): el logo "aterriza" con un rebote,
 * las ondas azul/naranja se expanden a sus lados, aparecen "Vibra" y el lema en
 * azul marino y, al final, el botón naranja "Comenzar rumbo" abajo del todo.
 */
@Composable
fun SplashScreen(onStart: () -> Unit) {
    val logoScale = remember { Animatable(0.7f) }
    val wordAlpha = remember { Animatable(0f) }
    val wordOffset = remember { Animatable(12f) }
    val buttonAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        delay(350)
        launch { wordAlpha.animateTo(1f, tween(450)) }
        launch { wordOffset.animateTo(0f, tween(450, easing = FastOutSlowInEasing)) }
        delay(450)
        buttonAlpha.animateTo(1f, tween(350))
    }

    val appName = stringResource(R.string.app_name)

    VibraBackground {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 22.dp),
        ) {
            // Las ondas solo ocupan los lados: el lienzo de 156 dp desborda en vertical
            // una caja más baja, así el texto queda cerca sin tocar la animación.
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(width = 156.dp, height = 98.dp)) {
                // Ondas del logo: azul a la izquierda, naranja a la derecha.
                BrandWaves(
                    modifier = Modifier.requiredSize(156.dp),
                    waves = 3,
                    startFraction = 0.32f,
                    travelFraction = 0.17f,
                    strokeWidth = 5.dp,
                    sweep = 76f,
                    periodMillis = 1_300,
                )
                // Insignia blanca con aro de marca: destaca sobre el marfil.
                VibraLogoBadge(
                    size = 90.dp,
                    ring = true,
                    fill = Color.White,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                        }
                        .semantics { contentDescription = appName },
                )
            }
            Text(
                text = appName,
                color = VibraColors.NavyInk,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.graphicsLayer {
                    alpha = wordAlpha.value
                    translationY = wordOffset.value.dp.toPx()
                },
            )
            Text(
                text = stringResource(R.string.splash_tagline),
                color = VibraColors.NavyInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.graphicsLayer { alpha = wordAlpha.value },
            )
        }
        EdgeButton(
            onClick = onStart,
            buttonSize = EdgeButtonSize.Small,
            colors = ButtonDefaults.buttonColors(
                containerColor = VibraColors.Orange,
                contentColor = VibraColors.NavyInk,
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { alpha = buttonAlpha.value },
        ) {
            Text(
                stringResource(R.string.splash_start),
                fontSize = 13.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@WearPreviewDevices
@Composable
private fun SplashPreview() {
    VibraTheme {
        AppScaffold { SplashScreen(onStart = {}) }
    }
}
