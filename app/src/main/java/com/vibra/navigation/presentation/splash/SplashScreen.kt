package com.vibra.navigation.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.vibra.navigation.R
import com.vibra.navigation.presentation.theme.VibraColors
import com.vibra.navigation.presentation.theme.VibraGradients
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash animado de marca. Continúa al splash del sistema (mismo logo sobre crema):
 * el logo "aterriza" con un rebote, ondas de vibración se expanden a sus lados y
 * aparece la palabra Vibra. Dura [SPLASH_DURATION_MS] y luego llama a [onFinished].
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val logoScale = remember { Animatable(0.7f) }
    val wordAlpha = remember { Animatable(0f) }
    val wordOffset = remember { Animatable(12f) }

    LaunchedEffect(Unit) {
        launch { logoScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        delay(350)
        launch { wordAlpha.animateTo(1f, tween(450)) }
        launch { wordOffset.animateTo(0f, tween(450, easing = FastOutSlowInEasing)) }
        delay(SPLASH_DURATION_MS - 350)
        finish()
    }

    val waves = rememberInfiniteTransition(label = "waves")
    val wave by waves.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_100, easing = LinearEasing), RepeatMode.Restart),
        label = "wave",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(VibraGradients.Splash),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                // Ondas de vibración que se expanden hacia los lados.
                Canvas(Modifier.size(150.dp)) {
                    for (i in 0 until 2) {
                        val t = (wave + i * 0.5f) % 1f
                        val radius = size.minDimension * (0.30f + 0.20f * t)
                        val alpha = (1f - t) * 0.8f
                        val topLeft = Offset(center.x - radius, center.y - radius)
                        val arcSize = Size(radius * 2, radius * 2)
                        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        drawArc(VibraColors.Orange.copy(alpha = alpha), 140f, 80f, false, topLeft, arcSize, style = stroke)
                        drawArc(VibraColors.Navy.copy(alpha = alpha), -40f, 80f, false, topLeft, arcSize, style = stroke)
                    }
                }
                Image(
                    painter = painterResource(R.drawable.vibra_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(84.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                        },
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                color = VibraColors.Navy,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.graphicsLayer {
                    alpha = wordAlpha.value
                    translationY = wordOffset.value.dp.toPx()
                },
            )
            Text(
                text = stringResource(R.string.splash_tagline),
                color = VibraColors.NavyMuted,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .graphicsLayer { alpha = wordAlpha.value },
            )
        }
    }
}

private const val SPLASH_DURATION_MS = 1_600L
