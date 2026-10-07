package com.dot.gallery.feature.editor.model

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterBAndW
import androidx.compose.material.icons.outlined.Gradient
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import kotlinx.serialization.Serializable
import com.dot.gallery.feature.editor.adjustments.varfilter.BlackWhite
import com.dot.gallery.feature.editor.adjustments.varfilter.BlueTone
import com.dot.gallery.feature.editor.adjustments.varfilter.Hue
import com.dot.gallery.feature.editor.adjustments.varfilter.Saturation
import com.dot.gallery.feature.editor.adjustments.varfilter.SkinTone
import com.dot.gallery.feature.editor.adjustments.varfilter.Tint
import com.dot.gallery.feature.editor.adjustments.varfilter.Warmth
import com.dot.gallery.feature.editor.generated.resources.Res
import com.dot.gallery.feature.editor.generated.resources.tool_black_white
import com.dot.gallery.feature.editor.generated.resources.tool_blue_tone
import com.dot.gallery.feature.editor.generated.resources.tool_hue
import com.dot.gallery.feature.editor.generated.resources.tool_saturation
import com.dot.gallery.feature.editor.generated.resources.tool_skin_tone
import com.dot.gallery.feature.editor.generated.resources.tool_tint
import com.dot.gallery.feature.editor.generated.resources.tool_warmth


@Keep
@Serializable
enum class ColourTool {
    Saturation,
    Warmth,
    Tint,
    SkinTone,
    BlueTone,
    Hue,
    BlackWhite;

    @get:Composable
    val translatedName: String
        get() = when (this) {
            Saturation -> stringResource(Res.string.tool_saturation)
            Warmth -> stringResource(Res.string.tool_warmth)
            Tint -> stringResource(Res.string.tool_tint)
            SkinTone -> stringResource(Res.string.tool_skin_tone)
            BlueTone -> stringResource(Res.string.tool_blue_tone)
            Hue -> stringResource(Res.string.tool_hue)
            BlackWhite -> stringResource(Res.string.tool_black_white)
        }

    val icon: ImageVector
        get() = when (this) {
            Saturation -> Icons.Outlined.WaterDrop
            Warmth -> Icons.Outlined.Thermostat
            Tint -> Icons.Outlined.Palette
            SkinTone -> Icons.Outlined.InvertColors
            BlueTone -> Icons.Outlined.Waves
            Hue -> Icons.Outlined.Gradient
            BlackWhite -> Icons.Outlined.FilterBAndW
        }

    val minValue: Float get() = -1f
    val maxValue: Float get() = 1f
    val defaultValue: Float get() = 0f
}
