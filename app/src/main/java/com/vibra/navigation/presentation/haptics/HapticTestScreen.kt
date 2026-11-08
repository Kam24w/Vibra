package com.vibra.navigation.presentation.haptics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.haptics.HapticPreset
import com.vibra.navigation.presentation.components.VibraBackground
import com.vibra.navigation.presentation.components.VibraButtons
import com.vibra.navigation.presentation.guidance.ManeuverIndicator
import com.vibra.navigation.presentation.theme.VibraColors
import com.vibra.navigation.presentation.theme.VibraTheme

@Composable
fun HapticTestRoute(viewModel: HapticTestViewModel = viewModel(factory = HapticTestViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HapticTestScreen(
        state = state,
        onPlay = viewModel::play,
        onIntensityChange = viewModel::changeIntensity,
    )
}

/** Test háptico: sentir izquierda vs derecha, patrones y ajustar la intensidad. */
@Composable
fun HapticTestScreen(
    state: HapticTestUiState,
    onPlay: (HapticPreset) -> Unit,
    onIntensityChange: (Int) -> Unit,
) {
    val listState = rememberScalingLazyListState()
    VibraBackground {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            ScalingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxWidth(),
            ) {
                item(key = "title") {
                    Text(
                        text = stringResource(R.string.haptics_title),
                        color = VibraColors.OnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                item(key = "sides") {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SideButton(
                            label = stringResource(R.string.haptics_left),
                            hint = stringResource(R.string.haptics_left_hint),
                            action = NavigationAction.TURN_LEFT,
                            highlighted = state.lastPlayed == HapticPreset.LEFT,
                            playCount = if (state.lastPlayed == HapticPreset.LEFT) state.playCount else 0,
                            onClick = { onPlay(HapticPreset.LEFT) },
                        )
                        SideButton(
                            label = stringResource(R.string.haptics_right),
                            hint = stringResource(R.string.haptics_right_hint),
                            action = NavigationAction.TURN_RIGHT,
                            highlighted = state.lastPlayed == HapticPreset.RIGHT,
                            playCount = if (state.lastPlayed == HapticPreset.RIGHT) state.playCount else 0,
                            onClick = { onPlay(HapticPreset.RIGHT) },
                        )
                    }
                }
                item(key = "quick") {
                    Button(
                        onClick = { onPlay(HapticPreset.PULSE) },
                        enabled = state.isVibratorAvailable,
                        colors = VibraButtons.primary(),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.haptics_quick_test), fontWeight = FontWeight.Bold) },
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
                item(key = "intensity_title") { SectionLabel(stringResource(R.string.haptics_intensity)) }
                item(key = "intensity") { IntensityStepper(state, onIntensityChange) }
                if (!state.hasAmplitudeControl || !state.isVibratorAvailable) {
                    item(key = "warning") {
                        Text(
                            text = stringResource(
                                if (!state.isVibratorAvailable) R.string.haptics_no_vibrator else R.string.haptics_no_amplitude,
                            ),
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

/** Botón circular grande (como los puños del mockup) que hace sentir un lado. */
@Composable
private fun SideButton(
    label: String,
    hint: String,
    action: NavigationAction,
    highlighted: Boolean,
    playCount: Int,
    onClick: () -> Unit,
) {
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(playCount) {
        if (playCount > 0) {
            bounce.snapTo(0.88f)
            bounce.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    val ring by animateColorAsState(if (highlighted) VibraColors.Orange else VibraColors.Outline, label = "ring")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = bounce.value
                    scaleY = bounce.value
                }
                .clip(CircleShape)
                .background(ring)
                .padding(4.dp)
                .clip(CircleShape)
                .background(VibraColors.Surface)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = "$label. $hint" },
        ) {
            ManeuverIndicator(action = action, eventId = 0L, color = VibraColors.OnSurface, size = 38.dp)
        }
        Text(label, color = VibraColors.OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(hint, color = VibraColors.OnSurfaceMuted, fontSize = 10.sp)
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

/** − ●●●○○ + : nivel de intensidad persistente (también afecta a la navegación). */
@Composable
private fun IntensityStepper(state: HapticTestUiState, onChange: (Int) -> Unit) {
    val description = stringResource(R.string.haptics_intensity_cd, state.intensityLevel, state.maxLevel)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
    ) {
        StepButton("−", enabled = state.intensityLevel > state.minLevel) { onChange(-1) }
        Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (level in state.minLevel..state.maxLevel) {
                val filled = level <= state.intensityLevel
                val color by animateColorAsState(if (filled) VibraColors.Orange else VibraColors.SurfaceHigh, label = "lvl")
                Box(
                    Modifier
                        .width(10.dp)
                        .height((10 + level * 3).dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color),
                )
            }
        }
        StepButton("+", enabled = state.intensityLevel < state.maxLevel) { onChange(1) }
    }
}

@Composable
private fun StepButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (enabled) VibraColors.SurfaceHigh else VibraColors.Surface)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Text(
            symbol,
            color = if (enabled) VibraColors.OnSurface else VibraColors.Disabled,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@WearPreviewDevices
@Composable
private fun HapticTestPreview() {
    VibraTheme {
        AppScaffold {
            HapticTestScreen(HapticTestUiState(intensityLevel = 3, lastPlayed = HapticPreset.RIGHT), {}, {})
        }
    }
}
