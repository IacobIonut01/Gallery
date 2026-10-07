/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer

object MediaActionCapabilityPolicy {
    fun resolve(input: MediaActionPolicyInput): MediaActionCapabilities {
        val cloudLocked = input.isCloud && input.cloudReadOnly
        val mutableLocalSource = input.sourceAllowsMutation &&
            !input.isCloud &&
            !input.isEncrypted &&
            !input.isReadUriOnly
        val exportAllowed = !cloudLocked

        return MediaActionCapabilities(
            share = exportAllowed,
            // Sending to a visual-search provider is the same data-leaving-device action as
            // share, plus it needs a still image — the source itself or the current video frame.
            visualSearch = exportAllowed && (input.isImage || input.isVideo),
            copyToClipboard = exportAllowed,
            favorite = !cloudLocked && when {
                input.isCloud -> input.providerSupportsFavorite
                else -> mutableLocalSource && input.platformSupportsFavorite
            },
            edit = mutableLocalSource,
            rotate = mutableLocalSource && input.isImage,
            rename = mutableLocalSource,
            trash = !cloudLocked && when {
                input.isCloud -> input.providerSupportsTrash || input.sourceAllowsDelete
                input.isEncrypted -> input.vaultDeleteAvailable
                else -> input.sourceAllowsDelete
            },
            hideInVault = mutableLocalSource,
            restoreFromVault = input.isEncrypted && input.vaultRestoreAvailable,
            copyOrMove = mutableLocalSource || input.isCloud,
            addToCollection = mutableLocalSource,
            setAlbumCover = mutableLocalSource,
            openExternally = !input.isEncrypted && !input.isCloud,
            download = input.isCloud,
            extractFrames = exportAllowed,
            exportMotionVideo = mutableLocalSource,
            cutout = input.isImage && exportAllowed,
        )
    }
}
