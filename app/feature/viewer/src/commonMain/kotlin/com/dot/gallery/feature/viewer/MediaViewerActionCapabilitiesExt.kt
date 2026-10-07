/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer

import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.canMakeActions
import com.dot.gallery.domain.util.isCloud
import com.dot.gallery.domain.util.isEncrypted
import com.dot.gallery.domain.util.isImage
import com.dot.gallery.domain.util.isVideo
import com.dot.gallery.domain.util.readUriOnly


/**
 * Resolves the viewer capability set for [Media]. The app supplies [cloudReadOnly]
 * (provider account settings lookup) and platform/provider capability bits.
 */
fun Media.viewerActionCapabilities(
    cloudReadOnly: Boolean = false,
    providerSupportsFavorite: Boolean = false,
    providerSupportsTrash: Boolean = false,
    platformSupportsFavorite: Boolean = false,
    sourceAllowsDelete: Boolean = canMakeActions,
    vaultRestoreAvailable: Boolean = false,
    vaultDeleteAvailable: Boolean = false,
): MediaActionCapabilities = MediaActionCapabilityPolicy.resolve(
    MediaActionPolicyInput(
        isCloud = isCloud,
        isEncrypted = isEncrypted,
        isImage = isImage,
        isVideo = isVideo,
        isReadUriOnly = readUriOnly,
        sourceAllowsMutation = canMakeActions,
        sourceAllowsDelete = sourceAllowsDelete,
        cloudReadOnly = cloudReadOnly,
        providerSupportsFavorite = providerSupportsFavorite,
        providerSupportsTrash = providerSupportsTrash,
        platformSupportsFavorite = platformSupportsFavorite,
        vaultRestoreAvailable = vaultRestoreAvailable,
        vaultDeleteAvailable = vaultDeleteAvailable,
    )
)
