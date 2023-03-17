package com.dot.gallery.feature_node.presentation.photos

import android.os.Parcelable
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotosState(
    val isLoading: Boolean = false,
    val media: List<Media> = emptyList(),
    val error: String = ""
) : Parcelable