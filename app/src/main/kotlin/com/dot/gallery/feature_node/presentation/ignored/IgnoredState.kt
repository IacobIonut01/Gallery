package com.dot.gallery.feature_node.presentation.ignored

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.dot.gallery.feature_node.domain.model.IgnoredAlbum
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class IgnoredState(
    val albums: List<IgnoredAlbum> = emptyList()
): Parcelable
