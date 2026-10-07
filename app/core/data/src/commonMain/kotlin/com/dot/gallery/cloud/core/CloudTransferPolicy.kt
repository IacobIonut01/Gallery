/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import com.dot.gallery.cloud.core.capabilities.remoteAlbumFilePath
import com.dot.gallery.cloud.core.CloudAlbumIdentity
import com.dot.gallery.cloud.core.ProviderType


fun isUnsupportedSameAccountCloudMove(
    source: CloudUri,
    destination: CloudAlbumIdentity,
    sourceLabel: String
): Boolean {
    if (source.providerType != destination.providerType ||
        source.configId != destination.serverConfigId
    ) return false
    if (destination.providerType == ProviderType.IMMICH) return true
    return runCatching {
        source.remoteId == remoteAlbumFilePath(destination.remoteId, sourceLabel)
    }.getOrDefault(true)
}

fun shouldDeleteCloudSourceAfterTransfer(
    source: CloudUri,
    destination: CloudAlbumIdentity,
    destinationRemoteId: String?
): Boolean = destinationRemoteId != null &&
    (source.providerType != destination.providerType ||
        source.configId != destination.serverConfigId ||
        source.remoteId != destinationRemoteId)
