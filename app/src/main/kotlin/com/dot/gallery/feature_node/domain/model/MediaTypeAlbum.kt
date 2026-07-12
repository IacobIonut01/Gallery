/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.domain.model

import com.dot.gallery.R
import com.dot.gallery.feature_node.domain.util.isGif
import com.dot.gallery.feature_node.domain.util.isImage
import com.dot.gallery.feature_node.domain.util.isRaw
import com.dot.gallery.feature_node.domain.util.isVideo

/**
 * Virtual "albums" that group the whole library by media type (Videos, Photos, GIFs, Raw).
 * They are not real MediaStore folders: each reserves a fixed negative [albumId] (in the
 * -601..-604 range, chosen to avoid every other reserved id such as -99, -200, -300, -500 and
 * the cloud range at -1000 and below) and is rendered as a tappable card in the Albums tab that
 * opens the standard album-timeline view filtered by [matches].
 */
enum class MediaTypeAlbum(val albumId: Long, val labelRes: Int) {
    VIDEOS(-601L, R.string.videos),
    PHOTOS(-602L, R.string.photos),
    GIFS(-603L, R.string.media_type_gifs),
    RAW(-604L, R.string.media_type_raw);

    fun matches(media: Media): Boolean = when (this) {
        VIDEOS -> media.isVideo
        PHOTOS -> media.isImage
        GIFS -> media.isGif
        RAW -> media.isRaw
    }

    companion object {
        const val ID_BASE = -600L

        fun fromAlbumId(id: Long): MediaTypeAlbum? = entries.firstOrNull { it.albumId == id }

        fun isMediaTypeAlbumId(id: Long): Boolean = entries.any { it.albumId == id }
    }
}
