package com.dot.gallery.feature_node.presentation.blacklist

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.dot.gallery.feature_node.domain.model.BlacklistedAlbum
import kotlinx.parcelize.Parcelize


@Immutable
@Parcelize
data class BlacklistState(
    val albums: List<BlacklistedAlbum> = emptyList()
): Parcelable
