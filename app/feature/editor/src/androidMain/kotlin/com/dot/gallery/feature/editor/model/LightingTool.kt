package com.dot.gallery.feature.editor.model

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness5
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.FilterDrama
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Tonality
import androidx.compose.material.icons.outlined.Vignette
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import kotlinx.serialization.Serializable
import com.dot.gallery.feature.editor.adjustments.varfilter.BlackPoint
import com.dot.gallery.feature.editor.adjustments.varfilter.Brightness
import com.dot.gallery.feature.editor.adjustments.varfilter.Highlights
import com.dot.gallery.feature.editor.adjustments.varfilter.Shadows
import com.dot.gallery.feature.editor.adjustments.varfilter.Tone
import com.dot.gallery.feature.editor.adjustments.varfilter.WhitePoint
import com.dot.gallery.feature.editor.generated.resources.Res
import com.dot.gallery.feature.editor.generated.resources.tool_black_point
import com.dot.gallery.feature.editor.generated.resources.tool_brightness
import com.dot.gallery.feature.editor.generated.resources.tool_contrast
import com.dot.gallery.feature.editor.generated.resources.tool_highlights
import com.dot.gallery.feature.editor.generated.resources.tool_shadows
import com.dot.gallery.feature.editor.generated.resources.tool_tone
import com.dot.gallery.feature.editor.generated.resources.tool_vignette
import com.dot.gallery.feature.editor.generated.resources.tool_white_point


@Keep
@Serializable
enum class LightingTool {
    Brightness,
    Tone,
    Contrast,
    BlackPoint,
    WhitePoint,
    Highlights,
    Shadows,
    Vignette;

    @get:Composable
    val translatedName: String
        get() = when (this) {
            Brightness -> stringResource(Res.string.tool_brightness)
            Tone -> stringResource(Res.string.tool_tone)
            Contrast -> stringResource(Res.string.tool_contrast)
            BlackPoint -> stringResource(Res.string.tool_black_point)
            WhitePoint -> stringResource(Res.string.tool_white_point)
            Highlights -> stringResource(Res.string.tool_highlights)
            Shadows -> stringResource(Res.string.tool_shadows)
            Vignette -> stringResource(Res.string.tool_vignette)
        }

    val icon: ImageVector
        get() = when (this) {
            Brightness -> Icons.Outlined.Brightness5
            Tone -> Icons.Outlined.Tonality
            Contrast -> Icons.Outlined.Contrast
            BlackPoint -> Icons.Outlined.RadioButtonUnchecked
            WhitePoint -> Icons.Outlined.Circle
            Highlights -> Icons.Outlined.Layers
            Shadows -> Icons.Outlined.FilterDrama
            Vignette -> Icons.Outlined.Vignette
        }

    val allowNegative: Boolean
        get() = this != Vignette

    val minValue: Float
        get() = if (allowNegative) -1f else 0f

    val maxValue: Float
        get() = 1f

    val defaultValue: Float
        get() = 0f
}
