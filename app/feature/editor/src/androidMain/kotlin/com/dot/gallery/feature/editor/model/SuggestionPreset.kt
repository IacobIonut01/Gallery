package com.dot.gallery.feature.editor.model

import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorMatrix
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.editor.adjustments.varfilter.Contrast
import com.dot.gallery.feature.editor.adjustments.varfilter.Saturation
import com.dot.gallery.feature.editor.generated.resources.Res
import com.dot.gallery.feature.editor.generated.resources.preset_dynamic
import com.dot.gallery.feature.editor.generated.resources.preset_enhance


@Keep
enum class SuggestionPreset {
    Enhance,
    Dynamic;

    @get:Composable
    val translatedName: String
        get() = when (this) {
            Enhance -> stringResource(Res.string.preset_enhance)
            Dynamic -> stringResource(Res.string.preset_dynamic)
        }

    fun colorMatrix(): ColorMatrix {
        return when (this) {
            Enhance -> {
                val brightness = 0.10f
                val contrast = 1.15f
                val saturation = 1.10f
                val cm = ColorMatrix()
                // Contrast
                cm.timesAssign(ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, 128f * (1 - contrast) + brightness * 255f,
                    0f, contrast, 0f, 0f, 128f * (1 - contrast) + brightness * 255f,
                    0f, 0f, contrast, 0f, 128f * (1 - contrast) + brightness * 255f,
                    0f, 0f, 0f, 1f, 0f
                )))
                // Saturation
                val s = saturation
                cm.timesAssign(ColorMatrix(floatArrayOf(
                    0.213f * (1 - s) + s, 0.715f * (1 - s), 0.072f * (1 - s), 0f, 0f,
                    0.213f * (1 - s), 0.715f * (1 - s) + s, 0.072f * (1 - s), 0f, 0f,
                    0.213f * (1 - s), 0.715f * (1 - s), 0.072f * (1 - s) + s, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )))
                cm
            }
            Dynamic -> {
                val contrast = 1.25f
                val saturation = 1.20f
                val cm = ColorMatrix()
                cm.timesAssign(ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, 128f * (1 - contrast),
                    0f, contrast, 0f, 0f, 128f * (1 - contrast),
                    0f, 0f, contrast, 0f, 128f * (1 - contrast),
                    0f, 0f, 0f, 1f, 0f
                )))
                val s = saturation
                cm.timesAssign(ColorMatrix(floatArrayOf(
                    0.213f * (1 - s) + s, 0.715f * (1 - s), 0.072f * (1 - s), 0f, 0f,
                    0.213f * (1 - s), 0.715f * (1 - s) + s, 0.072f * (1 - s), 0f, 0f,
                    0.213f * (1 - s), 0.715f * (1 - s), 0.072f * (1 - s) + s, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )))
                cm
            }
        }
    }
}
