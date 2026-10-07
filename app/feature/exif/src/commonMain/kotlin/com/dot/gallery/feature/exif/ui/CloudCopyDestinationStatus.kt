/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif.ui

import com.dot.gallery.cloud.core.CloudAlbumIdentity
import com.dot.gallery.cloud.core.matches
import org.jetbrains.compose.resources.StringResource
import com.dot.gallery.feature.exif.generated.resources.Res
import com.dot.gallery.feature.exif.generated.resources.cloud_copy_account_unavailable
import com.dot.gallery.feature.exif.generated.resources.cloud_copy_offline
import com.dot.gallery.feature.exif.generated.resources.cloud_copy_provider_unsupported
import com.dot.gallery.feature.exif.generated.resources.cloud_copy_read_only
import com.dot.gallery.feature.exif.generated.resources.cloud_copy_source_unsupported
import com.dot.gallery.feature.exif.generated.resources.cloud_move_unsupported
import com.dot.gallery.feature.exif.generated.resources.copy

enum class CloudCopyDestinationStatus {
    READY,
    MISSING_IDENTITY,
    SOURCE_UNSUPPORTED,
    MOVE_UNSUPPORTED,
    ACCOUNT_UNAVAILABLE,
    PROVIDER_UNSUPPORTED,
    READ_ONLY,
    OFFLINE
}

fun cloudCopyDestinationStatus(
    albumId: Long,
    identity: CloudAlbumIdentity?,
    sourcesSupported: Boolean,
    accountAvailable: Boolean,
    supportsAlbumWrite: Boolean,
    readOnly: Boolean,
    offline: Boolean
): CloudCopyDestinationStatus = when {
    identity?.matches(albumId) != true -> CloudCopyDestinationStatus.MISSING_IDENTITY
    !sourcesSupported -> CloudCopyDestinationStatus.SOURCE_UNSUPPORTED
    !accountAvailable -> CloudCopyDestinationStatus.ACCOUNT_UNAVAILABLE
    !supportsAlbumWrite -> CloudCopyDestinationStatus.PROVIDER_UNSUPPORTED
    readOnly -> CloudCopyDestinationStatus.READ_ONLY
    offline -> CloudCopyDestinationStatus.OFFLINE
    else -> CloudCopyDestinationStatus.READY
}

internal fun CloudCopyDestinationStatus.messageRes(): StringResource = when (this) {
    CloudCopyDestinationStatus.SOURCE_UNSUPPORTED -> Res.string.cloud_copy_source_unsupported
    CloudCopyDestinationStatus.MOVE_UNSUPPORTED -> Res.string.cloud_move_unsupported
    CloudCopyDestinationStatus.PROVIDER_UNSUPPORTED -> Res.string.cloud_copy_provider_unsupported
    CloudCopyDestinationStatus.READ_ONLY -> Res.string.cloud_copy_read_only
    CloudCopyDestinationStatus.OFFLINE -> Res.string.cloud_copy_offline
    CloudCopyDestinationStatus.READY -> Res.string.copy
    CloudCopyDestinationStatus.MISSING_IDENTITY,
    CloudCopyDestinationStatus.ACCOUNT_UNAVAILABLE -> Res.string.cloud_copy_account_unavailable
}
