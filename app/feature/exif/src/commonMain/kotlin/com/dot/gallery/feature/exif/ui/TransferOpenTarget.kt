/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif.ui

data class TransferOpenTarget(
    val type: TransferOpenTargetType,
    val albumId: Long,
    val albumLabel: String,
    val mediaId: Long? = null
)

fun resolveTransferOpenTarget(
    itemCount: Int,
    targetMediaId: Long?,
    destinationAlbumId: Long?,
    destinationLabel: String
): TransferOpenTarget? = when {
    itemCount == 1 && targetMediaId != null -> TransferOpenTarget(
        type = TransferOpenTargetType.MEDIA,
        albumId = destinationAlbumId ?: -1L,
        albumLabel = destinationLabel,
        mediaId = targetMediaId
    )
    destinationAlbumId != null -> TransferOpenTarget(
        type = TransferOpenTargetType.ALBUM,
        albumId = destinationAlbumId,
        albumLabel = destinationLabel
    )
    else -> null
}
