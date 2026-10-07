package com.dot.gallery.domain.model.editor

import androidx.compose.runtime.Immutable
import com.dot.gallery.domain.parcel.Parcelable
import com.dot.gallery.domain.parcel.Parcelize
import kotlinx.serialization.Serializable

@Immutable
@Serializable
@Parcelize
data class CropState(
    val isCropping: Boolean = false,
    val showCropper: Boolean = false
) : Parcelable