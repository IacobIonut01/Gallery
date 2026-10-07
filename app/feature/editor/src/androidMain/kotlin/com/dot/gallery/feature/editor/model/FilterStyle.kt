package com.dot.gallery.feature.editor.model

import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import kotlinx.serialization.Serializable
import com.dot.gallery.feature.editor.generated.resources.Res
import com.dot.gallery.feature.editor.generated.resources.filter_style_airy
import com.dot.gallery.feature.editor.generated.resources.filter_style_ember
import com.dot.gallery.feature.editor.generated.resources.filter_style_luminous
import com.dot.gallery.feature.editor.generated.resources.filter_style_radiant
import com.dot.gallery.feature.editor.generated.resources.filter_style_vivid


@Keep
@Serializable
enum class FilterStyle {
    Vivid,
    Luminous,
    Radiant,
    Ember,
    Airy;

    @get:Composable
    val translatedName: String
        get() = when (this) {
            Vivid -> stringResource(Res.string.filter_style_vivid)
            Luminous -> stringResource(Res.string.filter_style_luminous)
            Radiant -> stringResource(Res.string.filter_style_radiant)
            Ember -> stringResource(Res.string.filter_style_ember)
            Airy -> stringResource(Res.string.filter_style_airy)
        }

    val saturationDelta: Float
        get() = when (this) {
            Vivid -> 0.15f
            Luminous -> 0f
            Radiant -> 0.10f
            Ember -> 0f
            Airy -> -0.05f
        }

    val brightnessDelta: Float
        get() = when (this) {
            Vivid -> 0f
            Luminous -> 0.10f
            Radiant -> 0f
            Ember -> 0f
            Airy -> 0.10f
        }

    val contrastDelta: Float
        get() = when (this) {
            Vivid -> 0.05f
            Luminous -> 0f
            Radiant -> 0f
            Ember -> 0f
            Airy -> -0.10f
        }

    val warmthDelta: Float
        get() = when (this) {
            Vivid -> 0f
            Luminous -> 0f
            Radiant -> 0.10f
            Ember -> 0.15f
            Airy -> 0f
        }

    val highlightsDelta: Float
        get() = when (this) {
            Vivid -> 0f
            Luminous -> 0.05f
            Radiant -> 0f
            Ember -> -0.05f
            Airy -> 0f
        }
}
