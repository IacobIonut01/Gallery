/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.domain.model

import androidx.compose.runtime.Stable

@Stable
sealed class MediaItem {
    abstract val key: String

    @Stable
    data class Header(
        override val key: String,
        val text: String,
        val data: Set<Long>
    ) : MediaItem()

    @Stable
    data class MediaViewItem(
        override val key: String,
        val media: Media
    ) : MediaItem()

}

@Stable
sealed class EncryptedMediaItem {
    abstract val key: String

    @Stable
    data class Header(
        override val key: String,
        val text: String,
        val data: Set<Long>
    ) : EncryptedMediaItem()

    @Stable
    data class MediaViewItem(
        override val key: String,
        val media: DecryptedMedia
    ) : EncryptedMediaItem()

}

val Any.isHeaderKey: Boolean
    get() = this is String && this.startsWith("header_")

val Any.isBigHeaderKey: Boolean
    get() = this is String && this.startsWith("header_big_")

val Any.isIgnoredKey: Boolean
    get() = this is String && this == "aboveGrid"