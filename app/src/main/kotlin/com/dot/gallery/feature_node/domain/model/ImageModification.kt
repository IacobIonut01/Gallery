package com.dot.gallery.feature_node.domain.model

import android.graphics.Bitmap
import com.dot.gallery.feature_node.presentation.edit.components.adjustments.AdjustmentFilter

data class ImageModification(
    val croppedImage: Bitmap? = null,
    val filter: ImageFilter? = null,
    val adjustment: Pair<AdjustmentFilter, Float>? = null
)