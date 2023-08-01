/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.domain.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
sealed class MediaItem : Parcelable {
    abstract val key: String

    data class Header(
        override val key: String,
        val text: String,
        val data: List<Media>
    ) : MediaItem()

    @Parcelize
    sealed class MediaViewItem : MediaItem() {

        abstract val media: Media

        data class Loaded(
            override val key: String,
            override val media: Media,
        ) : MediaViewItem()
    }
}

val Any.isHeaderKey: Boolean
    get() = this is String && this.startsWith("header")

val Any.isSmallHeaderKey: Boolean
    get() = this is String && isHeaderKey && !this.contains("big")

val Any.isBigHeaderKey: Boolean
    get() = this is String && isHeaderKey && this.contains("big")

val Any.isIgnoredKey: Boolean
    get() = this is String && this == "aboveGrid"