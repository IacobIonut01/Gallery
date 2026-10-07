/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.edit.components.develop

import android.graphics.Bitmap
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.decoder.RawDemosaic
import com.dot.gallery.core.decoder.RawDevelopParams
import com.dot.gallery.core.decoder.RawHighlightMode
import com.dot.gallery.core.decoder.RawNoiseReduction
import com.dot.gallery.core.decoder.RawOutputColorSpace
import com.dot.gallery.core.decoder.RawWhiteBalance
import com.dot.gallery.domain.model.editor.DevelopCategory
import com.dot.gallery.core.util.or
import com.dot.gallery.feature.editor.adjustments.varfilter.Tone
import com.dot.gallery.feature.editor.components.develop.DevelopOptionTile
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.raw_color_adobe
import com.dot.gallery.generated.resources.raw_color_prophoto
import com.dot.gallery.generated.resources.raw_color_srgb
import com.dot.gallery.generated.resources.raw_color_wide
import com.dot.gallery.generated.resources.raw_colour_saturation
import com.dot.gallery.generated.resources.raw_colour_vibrance
import com.dot.gallery.generated.resources.raw_demosaic_ahd
import com.dot.gallery.generated.resources.raw_demosaic_dcb
import com.dot.gallery.generated.resources.raw_demosaic_linear
import com.dot.gallery.generated.resources.raw_demosaic_ppg
import com.dot.gallery.generated.resources.raw_demosaic_vng
import com.dot.gallery.generated.resources.raw_detail_noise_reduction
import com.dot.gallery.generated.resources.raw_detail_sharpen
import com.dot.gallery.generated.resources.raw_develop_color
import com.dot.gallery.generated.resources.raw_develop_demosaic
import com.dot.gallery.generated.resources.raw_develop_exposure
import com.dot.gallery.generated.resources.raw_develop_halfsize
import com.dot.gallery.generated.resources.raw_develop_highlight
import com.dot.gallery.generated.resources.raw_highlight_blend
import com.dot.gallery.generated.resources.raw_highlight_clip
import com.dot.gallery.generated.resources.raw_highlight_rebuild
import com.dot.gallery.generated.resources.raw_nr_high
import com.dot.gallery.generated.resources.raw_nr_low
import com.dot.gallery.generated.resources.raw_nr_medium
import com.dot.gallery.generated.resources.raw_nr_off
import com.dot.gallery.generated.resources.raw_tone_brightness
import com.dot.gallery.generated.resources.raw_tone_contrast
import com.dot.gallery.generated.resources.raw_tone_highlights
import com.dot.gallery.generated.resources.raw_tone_shadows
import com.dot.gallery.generated.resources.raw_wb_auto
import com.dot.gallery.generated.resources.raw_wb_camera
import com.dot.gallery.generated.resources.raw_wb_custom
import com.dot.gallery.generated.resources.raw_wb_daylight
import com.dot.gallery.generated.resources.raw_wb_temp
import com.dot.gallery.generated.resources.raw_wb_tint
import org.jetbrains.compose.resources.StringResource


/**
 * Renders the controls for a single RAW develop [category] (its own editor tab). Base-changing
 * option groups (white balance, demosaic, colour space, highlight, noise reduction) render as tiles
 * with a live cached preview thumbnail of that option applied to the current image; continuous
 * tone/detail controls render as labelled sliders. Every change flows through [onChange] which the
 * editor turns into an instant re-tone (tone-only) or a re-demosaic (base) under the hood.
 *
 * Control-heavy categories ([DevelopCategory.Tone], [DevelopCategory.Detail]) are capped to a fixed
 * height and scroll internally so they never shrink the image preview; the lighter categories size
 * to their content.
 */
@Composable
fun DevelopCategorySection(
    category: DevelopCategory,
    params: RawDevelopParams,
    onChange: (RawDevelopParams) -> Unit,
    thumbnailProvider: (suspend (RawDevelopParams) -> Bitmap?)?,
    modifier: Modifier = Modifier,
) {
    val content: @Composable () -> Unit = {
        when (category) {
            DevelopCategory.WhiteBalance -> WhiteBalanceSection(params, onChange, thumbnailProvider)
            DevelopCategory.Tone -> ToneSection(params, onChange)
            DevelopCategory.Detail -> DetailSection(params, onChange, thumbnailProvider)
            DevelopCategory.Colour -> ColourSection(params, onChange, thumbnailProvider)
            DevelopCategory.Output -> OutputSection(params, onChange, thumbnailProvider)
        }
    }

    val base = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    val columnModifier = if (category.fixedHeight) {
        base.height(CONTENT_HEIGHT).verticalScroll(rememberScrollState())
    } else {
        base
    }
    Column(
        modifier = modifier.then(columnModifier),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        content()
    }
}

/** Control-heavy categories scroll within a fixed height to protect the image preview. */
private val DevelopCategory.fixedHeight: Boolean
    get() = this == DevelopCategory.Tone || this == DevelopCategory.Detail

/** Fixed height of a scrollable category's controls — keeps the image preview large. */
private val CONTENT_HEIGHT = 208.dp

@Composable
private fun WhiteBalanceSection(
    params: RawDevelopParams,
    onChange: (RawDevelopParams) -> Unit,
    thumbs: (suspend (RawDevelopParams) -> Bitmap?)?,
) {
    TileRow {
        DevelopOptionTile(
            label = stringResource(Res.string.raw_wb_camera),
            selected = params.whiteBalance == RawWhiteBalance.CAMERA,
            optionParams = params.copy(whiteBalance = RawWhiteBalance.CAMERA),
            thumbnailProvider = thumbs,
        ) { onChange(params.copy(whiteBalance = RawWhiteBalance.CAMERA)) }
        DevelopOptionTile(
            label = stringResource(Res.string.raw_wb_auto),
            selected = params.whiteBalance == RawWhiteBalance.AUTO,
            optionParams = params.copy(whiteBalance = RawWhiteBalance.AUTO),
            thumbnailProvider = thumbs,
        ) { onChange(params.copy(whiteBalance = RawWhiteBalance.AUTO)) }
        DevelopOptionTile(
            label = stringResource(Res.string.raw_wb_daylight),
            selected = params.whiteBalance == RawWhiteBalance.DAYLIGHT,
            optionParams = params.copy(whiteBalance = RawWhiteBalance.DAYLIGHT),
            thumbnailProvider = thumbs,
        ) { onChange(params.copy(whiteBalance = RawWhiteBalance.DAYLIGHT)) }
        DevelopOptionTile(
            label = stringResource(Res.string.raw_wb_custom),
            selected = params.whiteBalance == RawWhiteBalance.CUSTOM,
            optionParams = params.copy(whiteBalance = RawWhiteBalance.CUSTOM, wbTempKelvin = params.wbTempKelvin ?: 5500),
            thumbnailProvider = thumbs,
        ) { onChange(params.copy(whiteBalance = RawWhiteBalance.CUSTOM, wbTempKelvin = params.wbTempKelvin ?: 5500)) }
    }
    if (params.whiteBalance == RawWhiteBalance.CUSTOM) {
        DevelopSlider(
            label = stringResource(Res.string.raw_wb_temp),
            value = (params.wbTempKelvin ?: 5500).toFloat(),
            valueRange = 2000f..12000f,
            valueFormatter = { "${it.toInt()}K" },
        ) { onChange(params.copy(wbTempKelvin = it.toInt(), userMul = null)) }
        DevelopSlider(
            label = stringResource(Res.string.raw_wb_tint),
            value = params.wbTint,
            valueRange = -1f..1f,
        ) { onChange(params.copy(wbTint = it, userMul = null)) }
    }
}

@Composable
private fun ToneSection(params: RawDevelopParams, onChange: (RawDevelopParams) -> Unit) {
    DevelopSlider(
        label = stringResource(Res.string.raw_develop_exposure),
        value = params.exposureEv,
        valueRange = -3f..3f,
        valueFormatter = { formatSigned(it) },
    ) { onChange(params.copy(exposureEv = it)) }
    DevelopSlider(
        label = stringResource(Res.string.raw_tone_brightness),
        value = params.brightness,
        valueRange = -2f..2f,
        valueFormatter = { formatSigned(it) },
    ) { onChange(params.copy(brightness = it)) }
    DevelopSlider(
        label = stringResource(Res.string.raw_tone_contrast),
        value = params.contrast,
        valueRange = -1f..1f,
    ) { onChange(params.copy(contrast = it)) }
    DevelopSlider(
        label = stringResource(Res.string.raw_tone_shadows),
        value = params.shadows,
        valueRange = -1f..1f,
    ) { onChange(params.copy(shadows = it)) }
    DevelopSlider(
        label = stringResource(Res.string.raw_tone_highlights),
        value = params.highlightsTone,
        valueRange = -1f..1f,
    ) { onChange(params.copy(highlightsTone = it)) }
}

@Composable
private fun DetailSection(
    params: RawDevelopParams,
    onChange: (RawDevelopParams) -> Unit,
    thumbs: (suspend (RawDevelopParams) -> Bitmap?)?,
) {
    SectionLabel(Res.string.raw_develop_demosaic)
    TileRow {
        demosaicOptions.forEach { (labelRes, algo) ->
            DevelopOptionTile(
                label = stringResource(labelRes),
                selected = params.demosaic == algo,
                optionParams = params.copy(demosaic = algo),
                thumbnailProvider = thumbs,
            ) { onChange(params.copy(demosaic = algo)) }
        }
    }
    DevelopSlider(
        label = stringResource(Res.string.raw_detail_sharpen),
        value = params.sharpen,
        valueRange = 0f..2f,
        valueFormatter = { formatOne(it) },
    ) { onChange(params.copy(sharpen = it)) }
    SectionLabel(Res.string.raw_detail_noise_reduction)
    TileRow {
        nrOptions.forEach { (labelRes, nr) ->
            DevelopOptionTile(
                label = stringResource(labelRes),
                selected = params.noiseReduction == nr,
                optionParams = params.copy(noiseReduction = nr),
                thumbnailProvider = thumbs,
            ) { onChange(params.copy(noiseReduction = nr)) }
        }
    }
    ToggleRow(
        label = stringResource(Res.string.raw_develop_halfsize),
        checked = params.halfSize,
    ) { onChange(params.copy(halfSize = it)) }
}

@Composable
private fun ColourSection(
    params: RawDevelopParams,
    onChange: (RawDevelopParams) -> Unit,
    thumbs: (suspend (RawDevelopParams) -> Bitmap?)?,
) {
    DevelopSlider(
        label = stringResource(Res.string.raw_colour_saturation),
        value = params.saturation,
        valueRange = -1f..1f,
    ) { onChange(params.copy(saturation = it)) }
    DevelopSlider(
        label = stringResource(Res.string.raw_colour_vibrance),
        value = params.vibrance,
        valueRange = -1f..1f,
    ) { onChange(params.copy(vibrance = it)) }
    SectionLabel(Res.string.raw_develop_color)
    TileRow {
        colorSpaceOptions.forEach { (labelRes, cs) ->
            DevelopOptionTile(
                label = stringResource(labelRes),
                selected = params.outputColorSpace == cs,
                optionParams = params.copy(outputColorSpace = cs),
                thumbnailProvider = thumbs,
            ) { onChange(params.copy(outputColorSpace = cs)) }
        }
    }
}

@Composable
private fun OutputSection(
    params: RawDevelopParams,
    onChange: (RawDevelopParams) -> Unit,
    thumbs: (suspend (RawDevelopParams) -> Bitmap?)?,
) {
    SectionLabel(Res.string.raw_develop_highlight)
    TileRow {
        highlightOptions.forEach { (labelRes, mode) ->
            DevelopOptionTile(
                label = stringResource(labelRes),
                selected = params.highlight == mode,
                optionParams = params.copy(highlight = mode),
                thumbnailProvider = thumbs,
            ) { onChange(params.copy(highlight = mode)) }
        }
    }
}

// ── Small building blocks ─────────────────────────────────────────────────────

@Composable
private fun TileRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top,
    ) { content() }
}

@Composable
private fun SectionLabel(res: StringResource) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DevelopSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueFormatter: (Float) -> String = { (it * 100).toInt().toString() },
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = Color.White)
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

private val demosaicOptions = listOf(
    Res.string.raw_demosaic_linear to RawDemosaic.LINEAR,
    Res.string.raw_demosaic_vng to RawDemosaic.VNG,
    Res.string.raw_demosaic_ppg to RawDemosaic.PPG,
    Res.string.raw_demosaic_ahd to RawDemosaic.AHD,
    Res.string.raw_demosaic_dcb to RawDemosaic.DCB,
)

private val nrOptions = listOf(
    Res.string.raw_nr_off to RawNoiseReduction.OFF,
    Res.string.raw_nr_low to RawNoiseReduction.LOW,
    Res.string.raw_nr_medium to RawNoiseReduction.MEDIUM,
    Res.string.raw_nr_high to RawNoiseReduction.HIGH,
)

private val colorSpaceOptions = listOf(
    Res.string.raw_color_srgb to RawOutputColorSpace.SRGB,
    Res.string.raw_color_adobe to RawOutputColorSpace.ADOBE_RGB,
    Res.string.raw_color_wide to RawOutputColorSpace.WIDE_GAMUT,
    Res.string.raw_color_prophoto to RawOutputColorSpace.PROPHOTO,
)

private val highlightOptions = listOf(
    Res.string.raw_highlight_clip to RawHighlightMode.CLIP,
    Res.string.raw_highlight_blend to RawHighlightMode.BLEND,
    Res.string.raw_highlight_rebuild to RawHighlightMode.REBUILD,
)

private fun formatSigned(v: Float): String = String.format("%+.1f", v)
private fun formatOne(v: Float): String = String.format("%.1f", v)
