/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import kotlinx.serialization.Serializable

@Serializable
enum class ProviderType(val displayName: String, val isRemote: Boolean) {
    IMMICH("Immich", isRemote = true),
    PHOTOPRISM("PhotoPrism", isRemote = true),
    OWNCLOUD("ownCloud", isRemote = true),
    NEXTCLOUD("Nextcloud", isRemote = true),
    WEBDAV("WebDAV", isRemote = true),
    SMB("SMB", isRemote = true),
    NFS("NFS", isRemote = true),
    LOCAL_PEOPLE("On-Device Faces", isRemote = false),
    LOCAL_OCR("On-Device OCR", isRemote = false),
    LOCAL_CLIP("On-Device Search", isRemote = false);

    companion object {
        fun remoteTypes(): List<ProviderType> = entries.filter { it.isRemote }
    }
}
