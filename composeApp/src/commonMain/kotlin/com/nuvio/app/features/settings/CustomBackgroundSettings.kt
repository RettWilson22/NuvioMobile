package com.nuvio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.Chip
import com.nuvio.app.core.ui.CustomBackgroundColorPresets
import com.nuvio.app.core.ui.CustomBackgroundGradientPresets
import com.nuvio.app.core.ui.CustomBackgroundMode
import com.nuvio.app.core.ui.CustomBackgroundRepository
import com.nuvio.app.core.ui.MaxCustomBackgroundBlur
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.rememberBackgroundImagePicker
import kotlin.math.roundToInt
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_appearance_section_background
import nuvio.composeapp.generated.resources.settings_background_blur
import nuvio.composeapp.generated.resources.settings_background_card_opacity
import nuvio.composeapp.generated.resources.settings_background_change_photo
import nuvio.composeapp.generated.resources.settings_background_choose_photo
import nuvio.composeapp.generated.resources.settings_background_choose_photo_description
import nuvio.composeapp.generated.resources.settings_background_dim
import nuvio.composeapp.generated.resources.settings_background_mode_color
import nuvio.composeapp.generated.resources.settings_background_mode_gradient
import nuvio.composeapp.generated.resources.settings_background_mode_image
import nuvio.composeapp.generated.resources.settings_background_mode_off
import nuvio.composeapp.generated.resources.settings_background_reset
import nuvio.composeapp.generated.resources.settings_background_style
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val CustomBackgroundMode.labelRes: StringResource
    get() = when (this) {
        CustomBackgroundMode.Off -> Res.string.settings_background_mode_off
        CustomBackgroundMode.SolidColor -> Res.string.settings_background_mode_color
        CustomBackgroundMode.Gradient -> Res.string.settings_background_mode_gradient
        CustomBackgroundMode.Image -> Res.string.settings_background_mode_image
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CustomBackgroundSettingsSection(isTablet: Boolean) {
    val state by remember {
        CustomBackgroundRepository.ensureLoaded()
        CustomBackgroundRepository.uiState
    }.collectAsStateWithLifecycle()
    val openPhotoPicker = rememberBackgroundImagePicker { bytes ->
        CustomBackgroundRepository.setImage(bytes)
    }
    val tokens = MaterialTheme.nuvio
    val horizontalPadding = if (isTablet) 20.dp else 16.dp

    SettingsSection(
        title = stringResource(Res.string.settings_appearance_section_background),
        isTablet = isTablet,
    ) {
        SettingsGroup(isTablet = isTablet) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_background_style),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.colors.textPrimary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CustomBackgroundMode.entries.forEach { mode ->
                        Chip(
                            label = stringResource(mode.labelRes),
                            selected = state.mode == mode,
                            onClick = {
                                if (mode == CustomBackgroundMode.Image && state.imageUrl == null) {
                                    openPhotoPicker()
                                } else {
                                    CustomBackgroundRepository.setMode(mode)
                                }
                            },
                        )
                    }
                }

                when (state.mode) {
                    CustomBackgroundMode.SolidColor -> SwatchRow(
                        count = CustomBackgroundColorPresets.size,
                        selectedIndex = state.colorIndex,
                        shape = CircleShape,
                        fill = { Modifier.background(CustomBackgroundColorPresets[it]) },
                        onSelect = CustomBackgroundRepository::setColorIndex,
                    )
                    CustomBackgroundMode.Gradient -> SwatchRow(
                        count = CustomBackgroundGradientPresets.size,
                        selectedIndex = state.gradientIndex,
                        shape = RoundedCornerShape(10.dp),
                        fill = {
                            Modifier.background(Brush.verticalGradient(CustomBackgroundGradientPresets[it]))
                        },
                        onSelect = CustomBackgroundRepository::setGradientIndex,
                    )
                    else -> Unit
                }
            }

            if (state.mode == CustomBackgroundMode.Image) {
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(
                        if (state.imageUrl == null) Res.string.settings_background_choose_photo
                        else Res.string.settings_background_change_photo,
                    ),
                    description = stringResource(Res.string.settings_background_choose_photo_description),
                    isTablet = isTablet,
                    trailingContent = {
                        state.imageUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 40.dp, height = 60.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    },
                    onClick = openPhotoPicker,
                )
            }

            if (state.mode != CustomBackgroundMode.Off) {
                SettingsGroupDivider(isTablet = isTablet)
                BackgroundSliderRow(
                    label = stringResource(Res.string.settings_background_dim),
                    value = state.dim,
                    range = 0f..90f,
                    suffix = "%",
                    horizontalPadding = horizontalPadding,
                    onCommit = CustomBackgroundRepository::setDim,
                )
                if (state.mode == CustomBackgroundMode.Image) {
                    BackgroundSliderRow(
                        label = stringResource(Res.string.settings_background_blur),
                        value = state.blur,
                        range = 0f..MaxCustomBackgroundBlur.toFloat(),
                        suffix = "",
                        horizontalPadding = horizontalPadding,
                        onCommit = CustomBackgroundRepository::setBlur,
                    )
                }
                BackgroundSliderRow(
                    label = stringResource(Res.string.settings_background_card_opacity),
                    value = state.cardOpacity,
                    range = 30f..100f,
                    suffix = "%",
                    horizontalPadding = horizontalPadding,
                    onCommit = CustomBackgroundRepository::setCardOpacity,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.settings_background_reset),
                    description = null,
                    isTablet = isTablet,
                    onClick = CustomBackgroundRepository::resetToDefaults,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchRow(
    count: Int,
    selectedIndex: Int,
    shape: Shape,
    fill: (Int) -> Modifier,
    onSelect: (Int) -> Unit,
) {
    val accent = MaterialTheme.nuvio.colors.accent
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(count) { index ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(shape)
                    .then(fill(index))
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) accent else Color.White.copy(alpha = 0.2f),
                        shape = shape,
                    )
                    .clickable { onSelect(index) },
            )
        }
    }
}

@Composable
private fun BackgroundSliderRow(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    onCommit: (Int) -> Unit,
) {
    var draft by remember { mutableFloatStateOf(value.toFloat()) }
    LaunchedEffect(value) { draft = value.toFloat() }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.nuvio.colors.textPrimary,
            )
            Text(
                text = "${draft.roundToInt()}$suffix",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.nuvio.colors.textMuted,
            )
        }
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = { onCommit(draft.roundToInt()) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
