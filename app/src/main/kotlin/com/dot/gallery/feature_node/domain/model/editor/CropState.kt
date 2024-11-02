package com.dot.gallery.feature_node.domain.model.editor

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Immutable
@Serializable
@Parcelize
data class CropState(
    val isCropping: Boolean = false,
    val showCropper: Boolean = false
) : Parcelable