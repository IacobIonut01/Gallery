package com.dot.gallery.feature_node.presentation.edit.adjustments.filters

import androidx.annotation.Keep
import com.dot.gallery.feature_node.domain.model.editor.ImageFilter
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class ImageFilterTypes {
    None, Monochrome, Negative, Cool, Warm, Sepia, Posterize, Vintage;

   fun createImageFilter(): ImageFilter =
        when (this) {
            Monochrome -> Monochrome()
            Negative -> Negative()
            Cool -> Cool()
            Warm -> Warm()
            Sepia -> Sepia()
            Posterize -> Posterize()
            Vintage -> Vintage()
            None -> None()
        }

}
