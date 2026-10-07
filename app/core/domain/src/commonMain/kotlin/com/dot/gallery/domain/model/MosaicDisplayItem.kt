/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

sealed class MosaicDisplayItem<T : Media> {
    abstract val key: String

    data class HeaderItem<T : Media>(val header: MediaItem.Header<T>) : MosaicDisplayItem<T>() {
        override val key = header.key
    }

    data class BigTileItem<T : Media>(
        val mediaItem: MediaItem.MediaViewItem<T>,
        val tileSize: BigTileSize,
        val dynamicSpan: Int = tileSize.colSpan,
        val dynamicAspectRatio: Float = tileSize.aspectRatio,
    ) : MosaicDisplayItem<T>() {
        override val key = "mosaic_big_${dynamicSpan}_${dynamicAspectRatio}_${mediaItem.key}"
    }

    data class QuadTileItem<T : Media>(val mediaItems: List<MediaItem.MediaViewItem<T>>) :
        MosaicDisplayItem<T>() {
        override val key = "mosaic_quad_${mediaItems.first().key}"
    }

    data class PairTileItem<T : Media>(val mediaItems: List<MediaItem.MediaViewItem<T>>) :
        MosaicDisplayItem<T>() {
        override val key = "mosaic_pair_${mediaItems.first().key}"
    }

    data class SingleItem<T : Media>(val mediaItem: MediaItem.MediaViewItem<T>) :
        MosaicDisplayItem<T>() {
        override val key = mediaItem.key
    }
}
