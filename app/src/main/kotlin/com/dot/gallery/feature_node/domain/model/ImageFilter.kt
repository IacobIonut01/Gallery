package com.dot.gallery.feature_node.domain.model

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

@Immutable
data class ImageFilter(
    val name: String = "",
    val filter: GPUImageFilter,
    val filterPreview: Bitmap
)