package com.dot.gallery.feature_node.presentation.edit.adjustments.varfilter

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.Rotate90DegreesCcw
import androidx.compose.ui.graphics.vector.ImageVector
import com.dot.gallery.feature_node.domain.model.editor.VariableFilter
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class VariableFilterTypes {
    Brightness, Contrast, Saturation, Rotate/*, Sharpness*/;

    fun createFilter(value: Float): VariableFilter =
        when (this) {
            Contrast -> Contrast(value)
            Saturation -> Saturation(value)
            //Sharpness -> Sharpness(value)
            Brightness -> Brightness(value)
            Rotate -> Rotate(value)
        }

    fun createDefaultFilter(): VariableFilter =
        when (this) {
            Contrast -> Contrast()
            Saturation -> Saturation()
            //Sharpness -> Sharpness()
            Brightness -> Brightness()
            Rotate -> Rotate()
        }

    val icon: ImageVector get() =
        when (this) {
            Brightness -> Icons.Outlined.Brightness4
            Contrast -> Icons.Outlined.Contrast
            Saturation -> Icons.Outlined.InvertColors
            Rotate -> Icons.Outlined.Rotate90DegreesCcw
            //Sharpness -> Icons.Outlined.Details
        }
}
