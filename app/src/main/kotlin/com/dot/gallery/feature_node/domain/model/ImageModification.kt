package com.dot.gallery.feature_node.domain.model

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import com.dot.gallery.feature_node.presentation.edit.components.adjustments.AdjustmentFilter

@Immutable
data class ImageModification(
    val croppedImage: Bitmap? = null,
    val filter: ImageFilter? = null,
    val adjustment: Pair<AdjustmentFilter, Float>? = null
)