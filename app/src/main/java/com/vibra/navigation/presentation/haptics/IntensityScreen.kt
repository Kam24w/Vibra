package com.vibra.navigation.presentation.haptics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.SliderDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.vibra.navigation.R
import com.vibra.navigation.data.settings.HapticSettingsRepository
import com.vibra.navigation.haptics.HapticPreset
import com.vibra.navigation.presentation.components.VibraBackground
import com.vibra.navigation.presentation.components.VibraButtons
import com.vibra.navigation.presentation.theme.VibraColors
import com.vibra.navigation.presentation.theme.VibraTheme

@Composable
fun IntensityRoute(viewModel: HapticTestViewModel = viewModel(factory = HapticTestViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    IntensityScreen(state = state, onChange = viewModel::setIntensity, onTry = { viewModel.play(HapticPreset.RIGHT) })
}

/**
 * Pantalla 5: calibración de la fuerza del motor con el Slider en línea de Wear OS
 * (botones −/+ y bisel giratorio), de 0 % a 100 % en pasos de 10. Cada cambio
 * vibra para sentir la nueva fuerza y se guarda al momento; también se aplica a
 * las indicaciones de navegación.
 */
@Composable
fun IntensityScreen(state: HapticTestUiState, onChange: (Int) -> Unit, onTry: () -> Unit) {
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 1)
    val percent = state.intensityPercent
    val percentText = stringResource(R.string.battery_percent, percent)
    val title = stringResource(R.string.haptics_intensity)
    VibraBackground {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            ScalingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item(key = "title") {
                    Text(title, color = VibraColors.OnSurfaceMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                item(key = "value") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedContent(
                            targetState = percent,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "intensityValue",
                        ) { value ->
                            Text(
                                text = stringResource(R.string.battery_percent, value),
                                color = if (value == 0) VibraColors.OnSurfaceMuted else VibraColors.OrangeText,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                        Text(
                            text = stringResource(
                                when {
                                    percent == 0 -> R.string.intensity_off
                                    percent <= 40 -> R.string.intensity_soft
                                    percent <= 70 -> R.string.intensity_medium
                                    else -> R.string.intensity_strong
                                },
                            ),
                            color = if (percent == 0) VibraColors.DangerText else VibraColors.OnSurfaceMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                item(key = "slider") {
                    Slider(
                        value = percent,
                        onValueChange = onChange,
                        valueProgression = HapticSettingsRepository.MIN_PERCENT..HapticSettingsRepository.MAX_PERCENT step
                            HapticSettingsRepository.STEP_PERCENT,
                        segmented = false,
                        colors = SliderDefaults.sliderColors(
                            containerColor = VibraColors.Surface,
                            buttonIconColor = VibraColors.OnSurface,
                            selectedBarColor = VibraColors.Orange,
                            unselectedBarColor = VibraColors.SurfaceHigh,
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = title
                            stateDescription = percentText
                        },
                    )
                }
                item(key = "try") {
                    Button(
                        onClick = onTry,
                        enabled = percent > 0 && state.isVibratorAvailable,
                        colors = VibraButtons.primary(),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.haptics_quick_test), fontWeight = FontWeight.Bold) },
                    )
                }
                if (!state.hasAmplitudeControl || !state.isVibratorAvailable) {
                    item(key = "warning") {
                        Text(
                            text = stringResource(
                                if (!state.isVibratorAvailable) R.string.haptics_no_vibrator else R.string.haptics_no_amplitude,
                            ),
                            color = VibraColors.OnSurfaceMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun IntensityPreview() {
    VibraTheme {
        AppScaffold { IntensityScreen(HapticTestUiState(intensityPercent = 70), {}, {}) }
    }
}
