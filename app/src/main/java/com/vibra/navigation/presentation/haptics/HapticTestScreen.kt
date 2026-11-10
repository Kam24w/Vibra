package com.vibra.navigation.presentation.haptics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.vibra.navigation.R
import com.vibra.navigation.haptics.HapticPreset
import com.vibra.navigation.presentation.components.HapticCommand
import com.vibra.navigation.presentation.components.HapticSignature
import com.vibra.navigation.presentation.components.VibraBackground
import com.vibra.navigation.presentation.components.VibraButtons
import com.vibra.navigation.presentation.components.VibraLogoBadge
import com.vibra.navigation.presentation.guidance.ManeuverIndicator
import com.vibra.navigation.presentation.theme.VibraColors
import com.vibra.navigation.presentation.theme.VibraGradients
import com.vibra.navigation.presentation.theme.VibraTheme

@Composable
fun HapticTestRoute(
    onOpenIntensity: () -> Unit,
    viewModel: HapticTestViewModel = viewModel(factory = HapticTestViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HapticTestScreen(state = state, onPlay = viewModel::play, onOpenIntensity = onOpenIntensity)
}

/**
 * Pantalla 4: los cuatro comandos hápticos de navegación (izquierda, derecha,
 * recto y regresar) en una cuadrícula 2×2 para sentirlos y aprenderlos, más los
 * patrones libres y el acceso al ajuste de intensidad.
 */
@Composable
fun HapticTestScreen(
    state: HapticTestUiState,
    onPlay: (HapticPreset) -> Unit,
    onOpenIntensity: () -> Unit,
) {
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 1)
    VibraBackground {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            ScalingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item(key = "title") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VibraLogoBadge(size = 22.dp)
                        Text(
                            text = stringResource(R.string.haptics_title),
                            color = VibraColors.OnSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                item(key = "row_turns") {
                    CommandRow(HapticCommand.LEFT, HapticCommand.RIGHT, state, onPlay)
                }
                item(key = "row_other") {
                    CommandRow(HapticCommand.STRAIGHT, HapticCommand.U_TURN, state, onPlay)
                }
                item(key = "intensity") {
                    Button(
                        onClick = onOpenIntensity,
                        colors = VibraButtons.brand(),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.haptics_intensity), fontWeight = FontWeight.Bold) },
                        secondaryLabel = {
                            Text(
                                if (state.intensityPercent == 0) {
                                    stringResource(R.string.intensity_off)
                                } else {
                                    stringResource(R.string.battery_percent, state.intensityPercent)
                                },
                            )
                        },
                    )
                }
                item(key = "patterns_title") { SectionLabel(stringResource(R.string.haptics_patterns)) }
                item(key = "patterns") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        PatternChip(stringResource(R.string.pattern_pulse), state.lastPlayed == HapticPreset.PULSE) {
                            onPlay(HapticPreset.PULSE)
                        }
                        PatternChip(stringResource(R.string.pattern_wave), state.lastPlayed == HapticPreset.WAVE) {
                            onPlay(HapticPreset.WAVE)
                        }
                        PatternChip(stringResource(R.string.pattern_rhythm), state.lastPlayed == HapticPreset.RHYTHM) {
                            onPlay(HapticPreset.RHYTHM)
                        }
                    }
                }
                if (!state.isVibratorAvailable) {
                    item(key = "warning") {
                        Text(
                            text = stringResource(R.string.haptics_no_vibrator),
                            color = VibraColors.OnSurfaceMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandRow(
    first: HapticCommand,
    second: HapticCommand,
    state: HapticTestUiState,
    onPlay: (HapticPreset) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        for (command in listOf(first, second)) {
            val selected = state.lastPlayed == command.preset
            CommandTile(
                command = command,
                highlighted = selected,
                playCount = if (selected) state.playCount else 0,
                onClick = { onPlay(command.preset) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Mosaico de comando: icono, nombre y la firma del ritmo; rebota al sentirlo. */
@Composable
private fun CommandTile(
    command: HapticCommand,
    highlighted: Boolean,
    playCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(playCount) {
        if (playCount > 0) {
            bounce.snapTo(0.9f)
            bounce.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    val border by animateColorAsState(
        if (highlighted) command.accent else VibraColors.Outline.copy(alpha = 0.6f),
        label = "commandBorder",
    )
    val label = stringResource(command.label)
    val hint = stringResource(command.hint)
    val shape = RoundedCornerShape(20.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                scaleX = bounce.value
                scaleY = bounce.value
            }
            .clip(shape)
            .background(VibraGradients.Card)
            .border(if (highlighted) 2.dp else 1.dp, border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$label. $hint" }
            .padding(vertical = 8.dp, horizontal = 6.dp),
    ) {
        ManeuverIndicator(action = command.action, eventId = 0L, color = command.accent, size = 30.dp)
        Text(label, color = VibraColors.OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        HapticSignature(
            waveform = command.preset.waveform(),
            color = command.accent,
            width = 52.dp,
            height = 10.dp,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/** Chip de patrón (Pulso / Onda / Ritmo). */
@Composable
private fun PatternChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) VibraColors.Orange else VibraColors.SurfaceHigh, label = "chip")
    Text(
        text = label,
        color = if (selected) VibraColors.OnOrange else VibraColors.OnSurface,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = VibraColors.OnSurfaceMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@WearPreviewDevices
@Composable
private fun HapticTestPreview() {
    VibraTheme {
        AppScaffold {
            HapticTestScreen(HapticTestUiState(intensityPercent = 80, lastPlayed = HapticPreset.RIGHT), {}, {})
        }
    }
}
